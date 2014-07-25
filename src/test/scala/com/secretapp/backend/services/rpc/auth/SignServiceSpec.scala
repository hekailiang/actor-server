package com.secretapp.backend.services.rpc.auth

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
import com.secretapp.backend.data.message.rpc.auth.{RequestSignIn, ResponseAuth, RequestSignUp}
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import com.newzly.util.testing.AsyncAssertionsHelper._
import scodec.bits._
import scalaz._
import Scalaz._
import scala.util.Random

class SignServiceSpec extends ActorLikeSpecification with CassandraSpecification with MockFactory with ActorServiceHelpers {
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

  "sign up" should {
    "success" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37721".bits
      val publicKeyHash = User.getPublicKeyHash(publicKey)
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy", Some("Klim"), publicKey)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      val user = struct.User(userId, accessHash, "Timothy", Some("Klim"), None, immutable.Seq(publicKeyHash))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(publicKeyHash, user)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "failed with new public key and same authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37721".bits
      val newPublicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37720".bits
      val firstName = "Timothy"
      val lastName = "Klim".some
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, firstName, lastName, newPublicKey)))
      val messageId = rand.nextLong()
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_PUBLIC_KEY", ""))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "success with new public key and new authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37721".bits
      val publicKeyHash = User.getPublicKeyHash(publicKey)
      val firstName = "Timothy"
      val lastName = "Klim".some
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val newPublicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004c16cf4eff9c8dc49745ad01566e7535d3a7ce09ba6ed1b012cf5f4bc5905435e08809649336621b5c4ec1a22ef4be591".bits
      val newPublicKeyHash = User.getPublicKeyHash(newPublicKey)
      val newAuthId = rand.nextLong()
      val newSessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(newAuthId, userId.some)).sync()
      SessionIdRecord.insertEntity(SessionId(newAuthId, newSessionId)).sync()
      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, firstName, lastName, newPublicKey)))
      val messageId = rand.nextLong()
      val packageBlob = codecRes2BS(protoPackageBox.build(newAuthId, newSessionId, messageId, rpcReq))
      probe.send(apiActor, Received(packageBlob))

      val accessHash = User.getAccessHash(newAuthId, userId, userSalt)
      val keyHashes = immutable.Seq(publicKeyHash, newPublicKeyHash)
      val newUser = user.copy(authId = newAuthId, publicKey = newPublicKey, publicKeyHash = newPublicKeyHash,
        keyHashes = keyHashes)
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(newPublicKeyHash, newUser.toStruct(newAuthId))))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)

      val users = UserRecord.getEntities(userId).map(_.sortBy(_.publicKeyHash))
      users must be_== (immutable.Seq(user.copy(keyHashes = keyHashes), newUser).sortBy(_.publicKeyHash)).await
    }
  }

  "sign in" should {
    "success" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = User.getPublicKeyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(publicKeyHash, user.toStruct(mockAuthId))))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "success with second public key and authId" in {
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = User.getPublicKeyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user)

      val newPublicKey = hex"ac1d".bits
      val newPublicKeyHash = User.getPublicKeyHash(publicKey)
      val newAuthId = rand.nextLong()
      val newSessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(newAuthId, userId.some)).sync()
      SessionIdRecord.insertEntity(SessionId(newAuthId, newSessionId)).sync()
      UserRecord.insertPartEntityWithPhoneAndPK(userId, newAuthId, newPublicKey, phoneNumber, userSalt).sync()

      Seq((mockAuthId, sessionId.id, publicKey), (newAuthId, newSessionId, newPublicKey)) foreach { (t) =>
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        implicit val (probe, apiActor) = probeAndActor()
        val (authId, sessionId, publicKey) = t
        val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey)))
        val messageId = rand.nextLong()
        val packageBlob = codecRes2BS(protoPackageBox.build(authId, sessionId, messageId, rpcReq))
        probe.send(apiActor, Received(packageBlob))

        val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(publicKeyHash, user.toStruct(authId))))
        val expectMsg = MessageBox(messageId, rpcRes)
        expectMsgWithAck(expectMsg)
      }
      success
    }

    "failed with new public key and valid authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = User.getPublicKeyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, hex"dead".bits)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_PUBLIC_KEY", ""))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }
  }
}
