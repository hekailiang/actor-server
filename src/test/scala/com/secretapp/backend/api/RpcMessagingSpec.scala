package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.update.{ CommonUpdate, MessageReceived, MessageSent }
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcSpec
import java.util.UUID
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.language.higherKinds
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class RpcMessagingSpec extends RpcSpec {
  import system.dispatcher

  def getState(implicit scope: TestScope): updateProto.State = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope

    val rq = updateProto.RequestGetState()
    val messageId = rand.nextLong()
    val rpcRq = RpcRequestBox(Request(rq))
    val packageBlob = pack(0, u.authId, MessageBox(messageId, rpcRq))
    send(packageBlob)

    val msg = receiveOneWithAck

    msg
      .body.asInstanceOf[RpcResponseBox]
      .body.asInstanceOf[Ok]
      .body.asInstanceOf[updateProto.State]
  }

  def getDifference(seq: Int, state: Option[UUID])(implicit scope: TestScope): updateProto.Difference = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope

    val rq = updateProto.RequestGetDifference(seq, state)
    val messageId = rand.nextLong()
    val rpcRq = RpcRequestBox(Request(rq))
    val packageBlob = pack(0, u.authId, MessageBox(messageId, rpcRq))
    send(packageBlob)

    val msg = receiveOneWithAck

    msg
      .body.asInstanceOf[RpcResponseBox]
      .body.asInstanceOf[Ok]
      .body.asInstanceOf[updateProto.Difference]
  }

  "RpcMessaging" should {
    "reply to SendMessage and push to sequence" in {
      //implicit val (probe, apiActor) = probeAndActor()
      //implicit val sessionId = SessionIdentifier()
      implicit val scope = TestScope()

      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val name = "Timothy Klim"
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = defaultPhoneNumber, name = name)
      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      authUser(user, defaultPhoneNumber)(scope.apiActor, scope.session)

      // insert second user
      val sndPublicKey = hex"ac1d3000".bits
      val sndUID = 3000
      val secondUser = User.build(uid = sndUID, authId = 333L, publicKey = sndPublicKey, accessSalt = userSalt,
        phoneNumber = defaultPhoneNumber, name = name)
      UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

      // get initial state
      val initialState = getState

      val rq = RequestSendMessage(
        uid = userId, accessHash = accessHash,
        randomId = 555L, useAesKey = false,
        aesMessage = None,
        messages = immutable.Seq(
          EncryptedMessage(uid = secondUser.uid, publicKeyHash = secondUser.publicKeyHash, None, Some(BitVector(1, 2, 3)))))
      val messageId = rand.nextLong()
      val rpcRq = RpcRequestBox(Request(rq))
      val packageBlob = pack(0, scope.user.authId, MessageBox(messageId, rpcRq))(scope.session)

      {
        send(packageBlob)(scope.probe, scope.apiActor)
        val msg = receiveOneWithAck()(scope.probe, scope.apiActor)
        val resp = msg
          .body.asInstanceOf[RpcResponseBox]
          .body.asInstanceOf[Ok]
          .body.asInstanceOf[ResponseSendMessage]

        val rsp = new ResponseSendMessage(
          mid = resp.mid,
          seq = initialState.seq + 1,
          state =
            resp.state)
        val rpcRes = RpcResponseBox(messageId, Ok(rsp))
        val expectMsg = MessageBox(messageId, rpcRes)

        msg must equalTo(expectMsg)
      }

      {
        val state = getState
        state.seq must equalTo(initialState.seq + 1)
        getDifference(initialState.seq, initialState.state).updates.length must equalTo(1)
      }

      Thread.sleep(1000)

      {
        send(packageBlob)(scope.probe, scope.apiActor)

        val msg = receiveOneWithAck()(scope.probe, scope.apiActor)

        val rpcRes = RpcResponseBox(messageId, Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false))
        val expectMsg = MessageBox(messageId, rpcRes)

        msg must equalTo(expectMsg)
      }

      getState.seq must equalTo(initialState.seq + 1)
    }

    "respond to RequestSendMessage with error if messages.length is zero" in {
      implicit val scope = TestScope()
      RequestSendMessage(1, User.getAccessHash(mockAuthId, 1, "salt"), 42, false, None, immutable.Seq()) :~> <~:(400, "ZERO_MESSAGES_LENGTH")
    }

    "send UpdateMessageReceived on RequestMessageReceived" in {
      val (scope1, scope2) = TestScope.pair()

      {
        implicit val scope = scope1

        val rq = RequestSendMessage(
          uid = scope2.user.uid, accessHash = scope2.user.accessHash(scope.user.authId),
          randomId = 555L, useAesKey = false,
          aesMessage = None,
          messages = immutable.Seq(
            EncryptedMessage(uid = scope2.user.uid, publicKeyHash = scope2.user.publicKeyHash, None, Some(BitVector(1, 2, 3)))))
        rq :~> <~:[ResponseSendMessage]

        // subscribe to updates
        getState(scope)
      }

      {
        implicit val scope = scope2

        RequestMessageReceived(scope1.user.uid, 555L, scope1.user.accessHash(scope.user.authId)) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[CommonUpdate]
        update.body should beAnInstanceOf[MessageReceived]
      }
    }
  }
}
