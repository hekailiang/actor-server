package com.secretapp.backend.services.rpc.auth

import akka.actor._
import akka.io.Tcp._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update.{ResponseSeq, RequestGetState}
import com.secretapp.backend.data.message.rpc.{Error, Ok, Request}
import com.secretapp.backend.data.message.{UpdateBox, RpcResponseBox, struct, RpcRequestBox}
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import scala.collection.immutable
import scala.language.{ postfixOps, higherKinds }
import scalaz.{State => _, _}
import Scalaz._
import scodec.bits._

class SignServiceSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>

    /*
    "auth code" should {
      "send sms code" in {
        implicit val scope = genTestScope()
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)

        sendRpcMsg(RequestAuthCode(phoneNumber, rand.nextInt(), rand.nextString(10)))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuthCode(_, false))) =>
        }
      }
    }
*/
    "sign up" should {
      /*
      "succeed and manage auths" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None) ))) =>
        }
      }*/


      "succeed and handle logout" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None) ))) =>
        }

        sendRpcMsg(RequestLogout())

        expectRpcMsg(Ok(ResponseVoid()))

        Thread.sleep(1000)
      }
/*
      "succeed with new public key and same authId" in {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key"))

        val accessHash = User.getAccessHash(scope.authId, scope.user.uid, scope.user.accessSalt)
        val newUser = struct.User(scope.user.uid, accessHash, scope.user.name, None, Set(newPublicKeyHash), scope.user.phoneNumber)
        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, newUser)), withNewSession = true)
      }

      "succeed with new public key and new authId" in {
        val scope = genTestScopeWithUser()
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = SessionIdentifier()
        implicit val newScope = scope.copy(authId = newAuthId, session = newSessionId)
        AuthIdRecord.insertEntity(AuthId(newAuthId, scope.user.uid.some)).sync()
        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key"))

        val keyHashes = Set(scope.user.publicKeyHash, newPublicKeyHash)
        val newUser = scope.user.copy(authId = newAuthId, publicKey = newPublicKey, publicKeyHash = newPublicKeyHash,
          keyHashes = keyHashes)
        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, newUser.toStruct(newAuthId))), withNewSession = true)

        def sortU(users: Seq[User]) = users.map(_.copy(keyHashes = keyHashes)).sortBy(_.publicKeyHash)

        val users = UserRecord.byUid(scope.user.uid).map(usrs => sortU(usrs))
        val expectUsers = sortU(immutable.Seq(scope.user, newUser))
        users must be_== (expectUsers).await
      }

      "succeed if name contains cyrillic characters" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(publicKeyHash)
        val phoneNumber = genPhoneNumber()
        val userId = rand.nextInt()
        val name = "Тимоти Клим"
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        PhoneRecord.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`publicKeyHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None) ))) =>
        }
      }

      "fail with invalid sms code" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, "invalid", smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "fail with invalid sms hash" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, "invalid", "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

      "fail with invalid first name if it is empty" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        PhoneRecord.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "   ", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should be nonempty", false), withNewSession = true)
      }

      "fail with invalid first name if it contains non printable characters" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        PhoneRecord.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "inv\u0001alid", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should contain printable characters only", false), withNewSession = true)
      }

      "fail with invalid public key if public key is empty" in {
        implicit val scope = genTestScope()
        val publicKey = BitVector.empty
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        PhoneRecord.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "INVALID_KEY", "", false), withNewSession = true)
      }

//      "fail with invalid public key if public key is invalid" in {
//        implicit val scope = genTestScope()
//        val publicKey = BitVector(hex"ac1d")
//        val phoneNumber = genPhoneNumber()
//        insertAuthId(scope.authId)
//        AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
//
//        val rpcReq = RpcRequestBox(Request(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy Klim", publicKey)))
//        val messageId = getMessageId()
//        val packageBlob = pack(0, authId, MessageBox(messageId, rpcReq))
//        send(packageBlob)
//
//        val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_KEY", "", false))
//        val expectMsg = MessageBox(messageId, rpcRes)
//        expectMsgWithAck(expectMsg)
//      }

      "send ContactRegistered notifications" in {
        val unregPhone = genPhoneNumber()
        val registered = genTestScopeWithUser()
        val unregistered = genTestScope()

        {
          implicit val scope = registered
          UnregisteredContactRecord.insertEntity(UnregisteredContact(unregPhone, scope.authId)).sync()

          sendRpcMsg(RequestGetState())

          expectMsgByPF(withNewSession = true) {
            case RpcResponseBox(_, Ok(ResponseSeq(0, None))) =>
          }
        }

        {
          val authId = rand.nextLong()
          implicit val scope = unregistered.copy(authId = authId)
          val publicKey = genPublicKey
          insertAuthId(authId)
          AuthSmsCodeRecord.insertEntity(AuthSmsCode(unregPhone, smsHash, smsCode)).sync()

          sendRpcMsg(RequestSignUp(unregPhone, smsHash, smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

          expectMsgByPF(withNewSession = true) {
            case RpcResponseBox(_, Ok(ResponseAuth(_, _))) =>
          }
        }

        {
          implicit val scope = registered

          expectMsgByPF() {
            case _: UpdateBox =>
          }
        }
      }*/
    }
/*
    "sign in" should {
      "success" in {
        implicit val scope = genTestScopeWithUser()
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Ok(ResponseAuth(scope.user.publicKeyHash, scope.user.toStruct(scope.authId))), withNewSession = true)
      }

      "success with second public key and authId" in {
        val session = SessionIdentifier()
        val authId = rand.nextLong()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        val userId = rand.nextInt()
        val user = User.build(uid = userId, authId = authId, publicKey = publicKey, accessSalt = userSalt,
          phoneNumber = phoneNumber, name = name)
        addUser(authId, session.id, user, phoneNumber)

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = rand.nextLong()
        AuthIdRecord.insertEntity(AuthId(newAuthId, userId.some)).sync()
        UserRecord.insertPartEntityWithPhoneAndPK(userId, newAuthId, newPublicKey, phoneNumber).sync()

        val newUser = user.copy(keyHashes = Set(publicKeyHash, newPublicKeyHash))
        val s = Seq((authId, session.id, publicKey, publicKeyHash), (newAuthId, newSessionId, newPublicKey, newPublicKeyHash))
        s foreach { t =>
          val (probe, apiActor) = getProbeAndActor()
          val (authId, sessionId, publicKey, publicKeyHash) = t
          implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(sessionId), authId = authId)
          AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

          sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, BitVector.empty, "app", 0, "key"))

          expectRpcMsg(Ok(ResponseAuth(publicKeyHash, newUser.toStruct(authId))), withNewSession = true)
        }
      }

      "success with new public key and valid authId" in {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()
        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, newPublicKey, BitVector.empty, "app", 0, "key"))

        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val keyHashes = Set(newPublicKeyHash)
        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`newPublicKeyHash`, struct.User(_, _, _, _, `keyHashes`, _, _) ))) =>
        }
      }

      "failed with invalid sms hash" in {
        implicit val scope = genTestScopeWithUser()
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, "invlid", smsCode, scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "failed with invalid sms code" in {
        implicit val scope = genTestScopeWithUser()
        AuthSmsCodeRecord.insertEntity(AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, "invalid", scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

//      "failed with invalid public key" in {
//        implicit val (probe, apiActor) = probeAndActor()
//        implicit val sessionId = SessionIdentifier()
//        implicit val authId = rand.nextLong()
//
//        val messageId = getMessageId()
//        val publicKey = genPublicKey
//        val user = User.build(uid = userId, authId = authId, publicKey = publicKey, accessSalt = userSalt,
//          phoneNumber = defaultPhoneNumber, name = "Timothy Klim")
//        addUser(authId, sessionId.id, user, defaultPhoneNumber)
//        AuthSmsCodeRecord.insertEntity(AuthSmsCode(defaultPhoneNumber, smsHash, smsCode)).sync()
//        catchNewSession()
//
//        val rpcReq = RpcRequestBox(Request(RequestSignIn(defaultPhoneNumber, smsHash, smsCode, hex"ac1d".bits)))
//        val packageBlob = pack(0, authId, MessageBox(messageId, rpcReq))
//        send(packageBlob)
//
//        val rpcRes = RpcResponseBox(messageId, Error(400, "INVALID_KEY", "", false))
//        val expectMsg = MessageBox(messageId, rpcRes)
//        expectMsgWithAck(expectMsg)
//      }
    }*/
  }
}
