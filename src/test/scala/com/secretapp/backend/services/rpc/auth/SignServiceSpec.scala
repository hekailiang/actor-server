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
import com.secretapp.backend.data.message.rpc.auth._
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

  "auth code" should {
    "send sms code" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      insertAuthAndSessionId()

      val rpcReq = RpcRequestBox(Request(RequestAuthCode(phoneNumber, rand.nextInt, rand.nextString(10))))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuthCode(smsHash, false)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }
  }

  "sign up" should {
    "success" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy", Some("Klim"), publicKey)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      val user = struct.User(userId, accessHash, "Timothy", Some("Klim"), None, Set(publicKeyHash), phoneNumber)
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(publicKeyHash, user)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "success with new public key and same authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = genPublicKey
      val newPublicKey = genPublicKey
      val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
      val firstName = "Timothy"
      val lastName = "Klim".some
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, firstName, lastName, newPublicKey)))
      val messageId = rand.nextLong()
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val accessHash = User.getAccessHash(mockAuthId, userId, userSalt)
      val newUser = struct.User(userId, accessHash, "Timothy", Some("Klim"), None, Set(newPublicKeyHash), phoneNumber)
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(newPublicKeyHash, newUser)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "success with new public key and new authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = "Klim".some
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val newPublicKey = genPublicKey
      val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
      val newAuthId = rand.nextLong()
      val newSessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(newAuthId, userId.some)).sync()
      SessionIdRecord.insertEntity(SessionId(newAuthId, newSessionId)).sync()
      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, firstName, lastName, newPublicKey)))
      val messageId = rand.nextLong()
      val packageBlob = codecRes2BS(protoPackageBox.build(newAuthId, newSessionId, messageId, rpcReq))
      probe.send(apiActor, Received(packageBlob))

      val accessHash = User.getAccessHash(newAuthId, userId, userSalt)
      val keyHashes = Set(publicKeyHash, newPublicKeyHash)
      val newUser = user.copy(authId = newAuthId, publicKey = newPublicKey, publicKeyHash = newPublicKeyHash,
        keyHashes = keyHashes)
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(newPublicKeyHash, newUser.toStruct(newAuthId))))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)

      def sortU(users: Seq[User]) = users.map(_.copy(keyHashes = keyHashes)).sortBy(_.publicKeyHash)

      val users = UserRecord.getEntities(userId).map(usrs => sortU(usrs))
      val expectUsers = sortU(immutable.Seq(user, newUser))
      users must be_== (expectUsers).await
    }

    "failed with invalid sms code" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, "invalid", smsCode, "Timothy", Some("Klim"), publicKey)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "PHONE_CODE_EXPIRED", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "failed with invalid sms hash" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, "invalid", "Timothy", Some("Klim"), publicKey)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "PHONE_CODE_INVALID", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    /*
    "failed with invalid public key" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy", Some("Klim"), publicKey)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_KEY", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    } */
  }

  "sign in" should {
    "success" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
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
      val publicKey = genPublicKey
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)

      val newPublicKey = genPublicKey
      val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
      val newAuthId = rand.nextLong()
      val newSessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(newAuthId, userId.some)).sync()
      SessionIdRecord.insertEntity(SessionId(newAuthId, newSessionId)).sync()
      UserRecord.insertPartEntityWithPhoneAndPK(userId, newAuthId, newPublicKey, phoneNumber).sync()

      val newUser = user.copy(keyHashes = Set(publicKeyHash, newPublicKeyHash))
      val s = Seq((mockAuthId, sessionId.id, publicKey, publicKeyHash), (newAuthId, newSessionId, newPublicKey, newPublicKeyHash))
      s foreach { (t) =>
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        implicit val (probe, apiActor) = probeAndActor()
        val (authId, sessionId, publicKey, publicKeyHash) = t
        val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey)))
        val messageId = rand.nextLong()
        val packageBlob = codecRes2BS(protoPackageBox.build(authId, sessionId, messageId, rpcReq))
        probe.send(apiActor, Received(packageBlob))

        val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(publicKeyHash, newUser.toStruct(authId))))
        val expectMsg = MessageBox(messageId, rpcRes)
        expectMsgWithAck(expectMsg)
      }
    }

    "success with new public key and valid authId" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = genPublicKey
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val newPublicKey = genPublicKey
      val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, newPublicKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val newUser = user.copy(keyHashes = Set(newPublicKeyHash))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(newPublicKeyHash, newUser.toStruct(mockAuthId))))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "failed with invalid sms code" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = genPublicKey
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, "invlid", smsCode, publicKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "PHONE_CODE_EXPIRED", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "failed with invalid sms hash" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = genPublicKey
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, "invlid", publicKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "PHONE_CODE_INVALID", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    /*
    "failed with invalid public key" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      addUser(mockAuthId, sessionId.id, user, phoneNumber)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_KEY", "", false))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    } */
  }
}
