package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.websudos.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.update.{ SeqUpdate, MessageReceived, MessageSent, MessageRead }
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcSpec
import java.util.UUID
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.higherKinds
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class RpcMessagingSpec extends RpcSpec {
  import system.dispatcher

  def getState(implicit scope: TestScope): (updateProto.ResponseSeq, Seq[UpdateBox]) = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope

    updateProto.RequestGetState() :~> <~:[updateProto.ResponseSeq]
  }

  def getDifference(seq: Int, state: Option[UUID])(implicit scope: TestScope): updateProto.Difference = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope

    val rq = updateProto.RequestGetDifference(seq, state)
    val messageId = getMessageId()
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
      val accessHash = User.getAccessHash(scope.user.authId, userId, userSalt)
      authUser(user, defaultPhoneNumber)

      // insert second user
      val sndPublicKey = hex"ac1d3000".bits
      val sndUID = 3000
      val secondUser = User.build(uid = sndUID, authId = 333L, publicKey = sndPublicKey, accessSalt = userSalt,
        phoneNumber = defaultPhoneNumber, name = name)
      UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

      /**
        * This sleep is needed to let sharding things to initialize
        * We catch an exception sometimes:
        * java.lang.IllegalArgumentException: requirement failed: Region Actor[akka.tcp://api@127.0.0.1:46807/user/sharding/Typing#1848681712] not registered: State(Map(),Map(Actor[akka.tcp://api@127.0.0.1:51745/user/sharding/Typing#1056784136] -> Vector()),Set())
        */
      Thread.sleep(1000)

      catchNewSession(scope)

      // get initial state
      val (initialState, _) = getState

      val rq = RequestSendMessage(
        uid = secondUser.uid, accessHash = secondUser.accessHash(scope.user.authId),
        randomId = 555L,
        message = EncryptedRSAMessage(
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              secondUser.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          )
        )
      )

      val (resp, _) = rq :~> <~:[updateProto.ResponseSeq]
      resp.seq should beEqualTo(initialState.seq + 2)

      val (state, _) = getState
      state.seq must equalTo(initialState.seq + 2)
      getDifference(initialState.seq, initialState.state).updates.length must equalTo(2)


      rq :~> <~:(409, "MESSAGE_ALREADY_SENT")

      Thread.sleep(1000)

      getState._1.seq must equalTo(initialState.seq + 2)
    }

    "send UpdateMessageReceived on RequestMessageReceived" in {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val rq = RequestSendMessage(
          uid = scope2.user.uid, accessHash = scope2.user.accessHash(scope.user.authId),
          randomId = 555L,
          message = EncryptedRSAMessage(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedAESKey(
                scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            ),
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            )
          )
        )
        rq :~> <~:[updateProto.ResponseSeq]

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
        val update = updBox.body.asInstanceOf[SeqUpdate]
        update.body should beAnInstanceOf[MessageReceived]
      }
    }

    "send UpdateMessageRead on RequestMessageRead" in {
      val (scope1, scope2) = TestScope.pair(5, 6)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val rq = RequestSendMessage(
          uid = scope2.user.uid, accessHash = scope2.user.accessHash(scope.user.authId),
          randomId = 555L,
          message = EncryptedRSAMessage(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedAESKey(
                scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            ),
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            )
          )
        )
        rq :~> <~:[updateProto.ResponseSeq]

        // subscribe to updates
        getState(scope)
      }

      Thread.sleep(500)

      {
        implicit val scope = scope2

        RequestMessageRead(scope1.user.uid, 555L, scope1.user.accessHash(scope.user.authId)) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[SeqUpdate]
        update.body should beAnInstanceOf[MessageRead]

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff.updates.last.body should beAnInstanceOf[MessageRead]
      }
    }

    "check provided keys" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      val scope2_2 = TestScope(scope2.user.uid, scope2.user.phoneNumber)

      catchNewSession(scope1)
      catchNewSession(scope2)
      catchNewSession(scope2_2)

      Await.result(UserPublicKeyRecord.setDeleted(scope2.user.uid, scope2.user.publicKeyHash), DurationInt(3).seconds)

      {
        implicit val scope = scope1

        val rq = RequestSendMessage(
          uid = scope2.user.uid, accessHash = scope2.user.accessHash(scope.user.authId),
          randomId = 555L,
          message = EncryptedRSAMessage(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedAESKey(
                scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            ),
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
              ),
              EncryptedAESKey(
                111L, BitVector(1, 0, 1, 0)
              )
            )
          )
        )
        val error = rq :~> <~:(400, "WRONG_KEYS")
        error.data.get should equalTo(struct.WrongReceiversErrorData(
          newKeys = Seq(struct.UserKey(scope2_2.user.uid, scope2_2.user.publicKeyHash)),
          removedKeys = Seq(struct.UserKey(scope2.user.uid, scope2.user.publicKeyHash)),
          invalidKeys = Seq(struct.UserKey(scope1.user.uid, 111L))
        ))
      }
    }
  }
}
