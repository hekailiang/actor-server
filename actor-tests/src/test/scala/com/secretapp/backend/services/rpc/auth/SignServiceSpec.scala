package com.secretapp.backend.services.rpc.auth

import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.{ UpdateBox, RpcResponseBox, struct }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import com.websudos.util.testing._
import scala.collection.immutable
import scala.language.{ postfixOps, higherKinds }
import scalaz.{State => _, _}
import Scalaz._
import scodec.bits._

class SignServiceSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>
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

    "sign up" should {
      "succeed" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(
            ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None, None) ))) =>
        }
      }

      "succeed and handle logout" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None, None) ))) =>
        }

        Thread.sleep(2000) // let database save user

        sendRpcMsg(RequestGetAuth())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuth(Seq(
            struct.AuthItem(_, 0, 0, "Android Official", "app", _, "", None, None)
          )))) =>
        }

        sendRpcMsg(RequestLogout())

        expectRpcMsg(Ok(ResponseVoid()))
      }
      "succeed with new public key and same authId" in {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key"))

        val accessHash = ACL.userAccessHash(scope.authId, scope.user)
        val newUser = struct.User(scope.user.uid, accessHash, scope.user.name, None, Set(newPublicKeyHash), scope.user.phoneNumber)
        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, newUser)), withNewSession = true)
      }

      "succeed with new public key and new authId" in {
        val scope = genTestScopeWithUser()
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = SessionIdentifier()
        implicit val newScope = scope.copy(authId = newAuthId, session = newSessionId)
        persist.AuthId.insertEntity(models.AuthId(newAuthId, scope.user.uid.some)).sync()
        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key"))

        val keyHashes = Set(scope.user.publicKeyHash, newPublicKeyHash)
        val newUser = scope.user.copy(authId = newAuthId, publicKey = newPublicKey, publicKeyHash = newPublicKeyHash,
          keyHashes = keyHashes)
        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, struct.User.fromModel(newUser, newAuthId))), withNewSession = true)

        def sortU(users: Seq[models.User]) = users.map(_.copy(keyHashes = keyHashes)).sortBy(_.publicKeyHash)

        val users = persist.User.byUid(scope.user.uid).map(usrs => sortU(usrs))
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
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`publicKeyHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None, None) ))) =>
        }
      }

      "fail with invalid sms code" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, "invalid", smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "fail with invalid sms hash" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, "invalid", "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

      "fail with invalid first name if it is empty" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "   ", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should be nonempty", false), withNewSession = true)
      }

      "fail with invalid first name if it contains non printable characters" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "inv\u0001alid", publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should contain printable characters only", false), withNewSession = true)
      }

      "fail with invalid public key if public key is empty" in {
        implicit val scope = genTestScope()
        val publicKey = BitVector.empty
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()
        persist.Phone.dropEntity(phoneNumber)

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
          persist.UnregisteredContact.insertEntity(models.UnregisteredContact(unregPhone, scope.user.uid)).sync()

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
          persist.AuthSmsCode.insertEntity(models.AuthSmsCode(unregPhone, smsHash, smsCode)).sync()

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
      }
    }

    "sign in" should {
      /*
      "success" in {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Ok(ResponseAuth(scope.user.publicKeyHash, struct.User.fromModel(scope.user, scope.authId))), withNewSession = true)
      }

      "success with second public key and authId" in {
        val session = SessionIdentifier()
        val authId = rand.nextLong()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        val userId = rand.nextInt()
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val user = models.User(userId, authId, pkHash, publicKey, phoneNumber, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
        addUser(authId, session.id, user, phoneNumber)

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = rand.nextLong()
        persist.AuthId.insertEntity(models.AuthId(newAuthId, userId.some)).sync()
        persist.User.insertEntityRowWithChildren(userId, newAuthId, newPublicKey, newPublicKeyHash, phoneNumber, name, "RU").sync()

        val newUser = user.copy(keyHashes = Set(publicKeyHash, newPublicKeyHash))
        val s = Seq((authId, session.id, publicKey, publicKeyHash), (newAuthId, newSessionId, newPublicKey, newPublicKeyHash))
        s foreach { t =>
          val (probe, apiActor) = getProbeAndActor()
          val (authId, sessionId, publicKey, publicKeyHash) = t
          implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(sessionId), authId = authId)
          persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

          sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, BitVector.empty, "app", 0, "key"))

          expectRpcMsg(Ok(ResponseAuth(publicKeyHash, struct.User.fromModel(newUser, authId))), withNewSession = true)
        }
      }
       */
      "remove old public key on sign up with the same authId and new public key" in {
        val session = new SessionIdentifier(rand.nextLong)
        val authId = rand.nextLong()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        val userId = rand.nextInt()
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val user = models.User(userId, authId, pkHash, publicKey, phoneNumber, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
        addUser(authId, session.id, user, phoneNumber)

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)

        persist.AuthId.insertEntity(models.AuthId(authId, userId.some)).sync()

        val newUser = user.copy(publicKey = newPublicKey, keyHashes = Set(newPublicKeyHash))

        val (probe, apiActor) = getProbeAndActor()
        implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = session, authId = authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, newPublicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, struct.User.fromModel(newUser, authId))), withNewSession = true)

        persist.UserPublicKey.getEntitiesByUserId(newUser.uid).sync() should equalTo(Seq(
          models.UserPublicKey(
            newUser.uid, newPublicKeyHash, newUser.accessSalt, newPublicKey, authId
          )
        ))

      }

      "success with new public key and valid authId" in {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()
        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, newPublicKey, BitVector.empty, "app", 0, "key"))

        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val keyHashes = Set(newPublicKeyHash)
        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`newPublicKeyHash`, struct.User(_, _, _, _, `keyHashes`, _, _, _) ))) =>
        }
      }

      "failed with invalid sms hash" in {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, "invlid", smsCode, scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "failed with invalid sms code" in {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(scope.user.phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, "invalid", scope.user.publicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

      "remove previous auth but keep keyHash on sign in with the same deviceHash and same keyHash" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector(1), "app1", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None, _) ))) =>
        }

        Thread.sleep(2000) // let database save user

        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, BitVector(1), "app2", 0, "key"))

        var userId: Integer = null

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(registeredUserId, _, `name`, None, `pkHashes`, `phoneNumber`, None, _) ))) =>
            userId = registeredUserId
        }

        Thread.sleep(2000) // let database process old auth deletion

        sendRpcMsg(RequestGetAuth())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuth(Seq(
            struct.AuthItem(_, 0, 0, "Android Official", "app2", _, "", None, None)
          )))) =>
        }

        val authItems = persist.AuthItem.getEntitiesByUserId(userId).sync()

        authItems.length should_== 1
        authItems.head.deviceTitle should_== "app2"

        val deletedAuthItems = persist.DeletedAuthItem.getEntitiesByUserId(userId).sync()

        deletedAuthItems.length should_== 1
        deletedAuthItems.head.deviceTitle should_== "app1"

        val pkeys = persist.UserPublicKey.getEntitiesByUserId(userId).sync()
        pkeys.length should_== 1

        val deletedPkeys = persist.UserPublicKey.getDeletedEntitiesByUserId(userId).sync()
        deletedPkeys.length should_== 0
      }

      "remove previous auth and mark keyHash as deleted on sign in with the same deviceHash" in {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector(1), "app1", 0, "key"))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, None, None) ))) =>
        }

        Thread.sleep(2000) // let database save user

        persist.AuthSmsCode.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode)).sync()

        val newPublicKey = genPublicKey
        val newPkHash = ec.PublicKey.keyHash(newPublicKey)
        val newPkHashes = Set(newPkHash)

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, newPublicKey, BitVector(1), "app2", 0, "key"))

        var userId: Integer = null

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseAuth(`newPkHash`, struct.User(registeredUserId, _, `name`, None, `newPkHashes`, `phoneNumber`, None, _) ))) =>
            userId = registeredUserId
        }

        Thread.sleep(2000) // let database process old auth deletion

        sendRpcMsg(RequestGetAuth())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuth(Seq(
            struct.AuthItem(_, 0, 0, "Android Official", "app2", _, "", None, None)
          )))) =>
        }

        val authItems = persist.AuthItem.getEntitiesByUserId(userId).sync()

        authItems.length should_== 1
        authItems.head.deviceTitle should_== "app2"

        val deletedAuthItems = persist.DeletedAuthItem.getEntitiesByUserId(userId).sync()

        deletedAuthItems.length should_== 1
        deletedAuthItems.head.deviceTitle should_== "app1"

        val pkeys = persist.UserPublicKey.getEntitiesByUserId(userId).sync()
        pkeys.length should_== 1
        pkeys.head.publicKeyHash should_== newPkHash

        val deletedPKeys = persist.UserPublicKey.getDeletedEntitiesByUserId(userId).sync()
        deletedPKeys.length should_== 1
        deletedPKeys.head.publicKeyHash should_== pkHash
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
    }
  }
}
