package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
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

  def getState(implicit sessionId: SessionIdentifier, probe: TestProbe, destActor: ActorRef): updateProto.State = {
    val rq = updateProto.RequestGetState()
    val messageId = rand.nextLong()
    val rpcRq = RpcRequestBox(Request(rq))
    val packageBlob = pack(MessageBox(messageId, rpcRq))
    send(packageBlob)

    val msg = receiveOneWithAck

    msg
      .body.asInstanceOf[RpcResponseBox]
      .body.asInstanceOf[Ok]
      .body.asInstanceOf[updateProto.State]
  }

  def getDifference(seq: Int, state: Option[UUID])(implicit sessionId: SessionIdentifier, probe: TestProbe, destActor: ActorRef): updateProto.Difference = {
    val rq = updateProto.RequestGetDifference(seq, state)
    val messageId = rand.nextLong()
    val rpcRq = RpcRequestBox(Request(rq))
    val packageBlob = pack(MessageBox(messageId, rpcRq))
    send(packageBlob)

    val msg = receiveOneWithAck

    msg
      .body.asInstanceOf[RpcResponseBox]
      .body.asInstanceOf[Ok]
      .body.asInstanceOf[updateProto.Difference]
  }

  "RpcMessaging" should {
    "reply to SendMessage and push to sequence" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()

      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      authUser(user, phoneNumber)

      // insert second user
      val sndPublicKey = hex"ac1d3000".bits
      val sndUID = 3000
      val secondUser = User.build(uid = sndUID, authId = 333L, publicKey = sndPublicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
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
      val packageBlob = pack(MessageBox(messageId, rpcRq))

      {
        send(packageBlob)
        val msg = receiveOneWithAck
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
        send(packageBlob)

        val msg = receiveOneWithAck

        val rpcRes = RpcResponseBox(messageId, Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false))
        val expectMsg = MessageBox(messageId, rpcRes)

        msg must equalTo(expectMsg)
      }

      getState.seq must equalTo(initialState.seq + 1)
    }
  }
}
