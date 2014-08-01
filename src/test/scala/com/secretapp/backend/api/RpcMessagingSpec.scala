package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.language.higherKinds
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class RpcMessagingSpec extends ActorLikeSpecification with CassandraSpecification with MockFactory with ActorServiceHelpers {
  import system.dispatcher

  override lazy val actorSystemName = "api"

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs () returning (12345L)
      }
    }
  }

  val smsCode = "test_sms_code"
  val smsHash = "test_sms_hash"
  val userId = 101
  val userSalt = "user_salt"

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
    override def genSmsCode = smsCode
    override def genSmsHash = smsHash
    override def genUserId = userId
    override def genUserAccessSalt = userSalt
  }

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

  "RpcMessaging" should {
    "reply to SendMessage and push to sequence" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = 123L, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      authUser(user, phoneNumber)

      // insert second user
      val sndPublicKey = hex"ac1d3000".bits
      val sndUID = 3000
      val secondUser = User.build(uid = sndUID, authId = 333L, publicKey = sndPublicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

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

        val rsp = new ResponseSendMessage(
          mid = 1,
          seq = 1,
          state =
            msg
              .body.asInstanceOf[RpcResponseBox]
              .body.asInstanceOf[Ok]
              .body.asInstanceOf[ResponseSendMessage]
              .state)
        val rpcRes = RpcResponseBox(messageId, Ok(rsp))
        val expectMsg = MessageBox(messageId, rpcRes)

        msg must equalTo(expectMsg)
      }

      {
        send(packageBlob)

        val msg = receiveOneWithAck

        val rpcRes = RpcResponseBox(messageId, Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false))
        val expectMsg = MessageBox(messageId, rpcRes)

        msg must equalTo(expectMsg)
      }
    }
  }
}
