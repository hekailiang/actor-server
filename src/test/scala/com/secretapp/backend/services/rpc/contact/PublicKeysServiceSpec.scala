package com.secretapp.backend.services.rpc.contact

import scala.language.{ postfixOps, higherKinds }
import scala.collection.immutable
import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.persist._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.data.message.{RpcResponseBox, struct, RpcRequestBox}
import com.secretapp.backend.data.message.rpc.{Error, Ok, Request}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.crypto.ec
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import com.newzly.util.testing.AsyncAssertionsHelper._
import scodec.bits._
import scalaz._
import Scalaz._
import scala.util.Random

class PublicKeysServiceSpec extends ActorLikeSpecification with CassandraSpecification with MockFactory with ActorServiceHelpers {
  import system.dispatcher

  override lazy val actorSystemName = "api"

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs() returning(12345L)
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

  "PublicKeysService" should {
    "return public keys" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val clientPhoneId = rand.nextLong()
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        firstName = firstName, lastName = lastName)
      authUser(user, phoneNumber)
      val secondUser = User.build(uid = userId + 1, authId = mockAuthId + 1, publicKey = publicKey, accessSalt = userSalt,
        firstName = firstName, lastName = lastName)
      val accessHash = User.getAccessHash(mockAuthId, secondUser.uid, secondUser.accessSalt)
      UserRecord.insertEntityWithPhoneAndPK(secondUser, phoneNumber + 1).sync()

      val reqKeys = immutable.Seq(PublicKeyRequest(secondUser.uid, accessHash, secondUser.publicKeyHash))
      val rpcReq = RpcRequestBox(Request(RequestPublicKeys(reqKeys)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val resKeys = immutable.Seq(PublicKeyResponse(secondUser.uid, secondUser.publicKeyHash, secondUser.publicKey))
      val resBody = ResponsePublicKeys(resKeys)
      val rpcRes = RpcResponseBox(messageId, Ok(resBody))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }
  }
}
