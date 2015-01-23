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
import scala.concurrent.Future
import scala.language.{ postfixOps, higherKinds }
import scalaz.{State => _, _}
import Scalaz._
import scodec.bits._

class SignServiceSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>
    "auth code" should {
      "send sms code" in new sqlDb {
        implicit val scope = genTestScope()
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)

        sendRpcMsg(RequestSendAuthCode(phoneNumber, rand.nextInt(), rand.nextString(10)))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseSendAuthCode(_, false))) =>
        }
      }
    }

    "sign up" should {
      "succeed" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key", false))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(
            ResponseAuth(
              `pkHash`,
              struct.User(
                _,
                _,
                `name`,
                None,
                `pkHashes`,
                `phoneNumber`,
                phoneIds,
                _,
                models.UserState.Registered,
                None,
                None
              ), struct.Config(300)
            ))) if (!phoneIds.isEmpty) =>
        }
      }

      "succeed and handle logout" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key", false))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(
            `pkHash`,
            struct.User(
              _,
              _,
              `name`,
              None,
              `pkHashes`,
              `phoneNumber`,
              phoneIds,
              _,
              models.UserState.Registered,
              None,
              None
            ), struct.Config(300)))) if (!phoneIds.isEmpty) =>
        }

        Thread.sleep(2000) // let database save user

        sendRpcMsg(RequestGetAuthSessions())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuthSessions(Seq(
            struct.AuthSession(_, 0, 0, "Android Official", "app", _, "", None, None)
          )))) =>
        }

        sendRpcMsg(RequestSignOut())

        expectRpcMsg(Ok(ResponseVoid()))
      }
      "succeed with new public key and same authId" in new sqlDb {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key", false))

        val accessHash = ACL.userAccessHash(scope.authId, scope.user)
        val keyHashes = Set(newPublicKeyHash)

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(
            newPublicKeyHash,
            struct.User(
              scope.user.uid,
              accessHash,
              scope.user.name,
              None,
              keyHashes,
              scope.user.phoneNumber,
              phoneIds,
              _,
              models.UserState.Registered,
              None,
              None
            ),
            struct.Config(300)
          ))) if (!phoneIds.isEmpty) =>
        }
      }

      "succeed with new public key and new authId" in new sqlDb {
        val scope = genTestScopeWithUser()
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = SessionIdentifier()
        implicit val newScope = scope.copy(authId = newAuthId, session = newSessionId)
        persist.AuthId.create(newAuthId, scope.user.uid.some).sync()
        sendRpcMsg(RequestSignUp(scope.user.phoneNumber, smsHash, smsCode, scope.user.name, newPublicKey, BitVector.empty, "app", 0, "key", false))

        val keyHashes = Set(scope.user.publicKeyHash, newPublicKeyHash)
        val newUser = scope.user.copy(authId = newAuthId, publicKeyData = newPublicKey, publicKeyHash = newPublicKeyHash,
          publicKeyHashes = keyHashes)
        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, struct.User.fromModel(newUser, models.AvatarData.empty, newAuthId), struct.Config(300))), withNewSession = true)

        def sortU(users: Seq[models.User]) = users.map(_.copy(publicKeyHashes = keyHashes)).sortBy(_.publicKeyHash)

        val users = persist.UserPublicKey.findAllByUserId(scope.user.uid) flatMap { keys =>
          keys match {
            case firstKey :: _ =>
              for {
                userOpt <- persist.User.find(scope.user.uid)(authId = Some(firstKey.authId))
              } yield userOpt map { user =>
                keys map { key =>
                  user.copy(
                    authId = key.authId,
                    publicKeyHash = key.hash,
                    publicKeyData = key.data
                  )
                }
              } getOrElse (Seq.empty)
            case Nil => Future.successful(Seq.empty)
          }
        } map sortU

        //val users = persist.User.byUid(scope.user.uid).map(usrs => sortU(usrs))
        val expectUsers = sortU(immutable.Seq(scope.user, newUser))

        users must be_== (expectUsers).await
      }

      "succeed if name contains cyrillic characters" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(publicKeyHash)
        val phoneNumber = genPhoneNumber()
        val userId = rand.nextInt()
        val name = "Тимоти Клим"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()
        //persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector.empty, "app", 0, "key", false))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(
            `publicKeyHash`,
            struct.User(
              _,
              _,
              `name`,
              None,
              `pkHashes`,
              `phoneNumber`,
              phoneIds,
              _,
              models.UserState.Registered,
              None,
              None
            ), struct.Config(300)))) if (!phoneIds.isEmpty) =>
        }
      }

      "fail with invalid sms code" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, "invalid", smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key", false))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "fail with invalid sms hash" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, "invalid", "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key", false))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

      "fail with invalid first name if it is empty" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()
        //persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "   ", publicKey, BitVector.empty, "app", 0, "key", false))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should be nonempty", false), withNewSession = true)
      }

      "fail with invalid first name if it contains non printable characters" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()
        //persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "inv\u0001alid", publicKey, BitVector.empty, "app", 0, "key", false))

        expectRpcMsg(Error(400, "NAME_INVALID", "Should contain printable characters only", false), withNewSession = true)
      }

      "fail with invalid public key if public key is empty" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = BitVector.empty
        val phoneNumber = genPhoneNumber()
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()
        //persist.Phone.dropEntity(phoneNumber)

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key", false))

        expectRpcMsg(Error(400, "INVALID_KEY", "", false), withNewSession = true)
      }

//      "fail with invalid public key if public key is invalid" in new sqlDb {
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

      "send ContactRegistered notifications" in new sqlDb {
        val unregPhone = genPhoneNumber()
        val registered = genTestScopeWithUser()
        val unregistered = genTestScope()

        {
          implicit val scope = registered
          persist.UnregisteredContact.createIfNotExists(unregPhone, scope.user.uid).sync()

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
          persist.AuthSmsCode.create(unregPhone, smsHash, smsCode).sync()

          sendRpcMsg(RequestSignUp(unregPhone, smsHash, smsCode, "Timothy Klim", publicKey, BitVector.empty, "app", 0, "key", false))

          expectMsgByPF(withNewSession = true) {
            case RpcResponseBox(_, Ok(ResponseAuth(_, _, _))) =>
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
      "success" in new sqlDb {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, scope.user.publicKeyData, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Ok(ResponseAuth(scope.user.publicKeyHash, struct.User.fromModel(scope.user, models.AvatarData.empty, scope.authId), struct.Config(300))), withNewSession = true)
      }

      "success with second public key and authId" in new sqlDb {
        val session = SessionIdentifier()
        val authId = rand.nextLong()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        val userId = rand.nextInt()
        val pkHash = ec.PublicKey.keyHash(publicKey)

        val phoneId = rand.nextInt
        val phone = models.UserPhone(phoneId, userId, phoneSalt, phoneNumber, "Mobile phone")

        val user = models.User(
          userId,
          authId,
          pkHash,
          publicKey,
          phoneNumber,
          userSalt,
          name,
          "RU",
          models.NoSex,
          publicKeyHashes = immutable.Set(pkHash),
          phoneIds = immutable.Set(phoneId),
          emailIds = immutable.Set.empty,
          state = models.UserState.Registered
        )
        addUser(authId, session.id, user, phone)

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val newAuthId = rand.nextLong()
        val newSessionId = rand.nextLong()
        persist.AuthId.create(newAuthId, userId.some).sync()
        persist.User.savePartial(id = userId, name = name, countryCode = "RU")(
          authId = newAuthId,
          publicKeyHash = newPublicKeyHash,
          publicKeyData = newPublicKey,
          phoneNumber = phoneNumber
        ).sync()

        val newUser = user.copy(publicKeyHashes = Set(publicKeyHash, newPublicKeyHash))
        val s = Seq((authId, session.id, publicKey, publicKeyHash), (newAuthId, newSessionId, newPublicKey, newPublicKeyHash))
        s foreach { t =>
          val (probe, apiActor) = getProbeAndActor()
          val (authId, sessionId, publicKey, publicKeyHash) = t
          implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(sessionId), authId = authId)
          persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

          sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, BitVector.empty, "app", 0, "key"))

          expectRpcMsg(Ok(ResponseAuth(publicKeyHash, struct.User.fromModel(newUser, models.AvatarData.empty, authId), struct.Config(300))), withNewSession = true)
        }
      }

      "remove old public key on sign up with the same authId and new public key" in new sqlDb {
        val session = new SessionIdentifier(rand.nextLong)
        val authId = rand.nextLong()
        val publicKey = genPublicKey
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        val userId = rand.nextInt()
        val pkHash = ec.PublicKey.keyHash(publicKey)

        val phoneId = rand.nextInt
        val phone = models.UserPhone(phoneId, userId, phoneSalt, phoneNumber, "Mobile phone")

        val user = models.User(
          userId,
          authId,
          pkHash,
          publicKey,
          phoneNumber,
          userSalt,
          name,
          "RU",
          models.NoSex,
          publicKeyHashes = immutable.Set(pkHash),
          phoneIds = immutable.Set(phoneId),
          emailIds = immutable.Set.empty,
          state = models.UserState.Registered
        )
        addUser(authId, session.id, user, phone)

        val newPublicKey = genPublicKey
        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)

        persist.AuthId.createOrUpdate(authId, userId.some).sync()

        val newUser = user.copy(publicKeyData = newPublicKey, publicKeyHashes = Set(newPublicKeyHash))

        val (probe, apiActor) = getProbeAndActor()
        implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = session, authId = authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, newPublicKey, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Ok(ResponseAuth(newPublicKeyHash, struct.User.fromModel(newUser, models.AvatarData.empty, authId), struct.Config(300))), withNewSession = true)

        persist.UserPublicKey.findAllByUserId(newUser.uid).sync() should equalTo(Seq(
          models.UserPublicKey(
            newUser.uid, newPublicKeyHash, newPublicKey, authId
          )
        ))

      }

      "success with new public key and valid authId" in new sqlDb {
        implicit val scope = genTestScopeWithUser()
        val newPublicKey = genPublicKey
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()
        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, smsCode, newPublicKey, BitVector.empty, "app", 0, "key"))

        val newPublicKeyHash = ec.PublicKey.keyHash(newPublicKey)
        val keyHashes = Set(newPublicKeyHash)
        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`newPublicKeyHash`, struct.User(_, _, _, _, `keyHashes`, _, _, _, _, _, _), _ ))) =>
        }
      }

      "failed with invalid sms hash" in new sqlDb {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, "invlid", smsCode, scope.user.publicKeyData, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_EXPIRED", "", false), withNewSession = true)
      }

      "failed with invalid sms code" in new sqlDb {
        implicit val scope = genTestScopeWithUser()
        persist.AuthSmsCode.create(scope.user.phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignIn(scope.user.phoneNumber, smsHash, "invalid", scope.user.publicKeyData, BitVector.empty, "app", 0, "key"))

        expectRpcMsg(Error(400, "PHONE_CODE_INVALID", "", false), withNewSession = true)
      }

      "remove previous auth but keep keyHash on sign in with the same deviceHash and same keyHash" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector(1), "app1", 0, "key", false))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, _, _, _, None, _), _ ))) =>
        }

        Thread.sleep(2000) // let database save user

        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, BitVector(1), "app2", 0, "key"))

        var userId: Integer = null

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(registeredUserId, _, `name`, None, `pkHashes`, `phoneNumber`, _, _, _, None, _), _ ))) =>
            userId = registeredUserId
        }

        Thread.sleep(2000) // let database process old auth deletion

        sendRpcMsg(RequestGetAuthSessions())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuthSessions(Seq(
            struct.AuthSession(_, 0, 0, "Android Official", "app2", _, "", None, None)
          )))) =>
        }

        val authItems = persist.AuthSession.findAllByUserId(userId).sync()

        authItems.length should_== 1
        authItems.head.deviceTitle should_== "app2"

        val deletedAuthSessions = persist.AuthSession.findAllDeletedByUserId(userId).sync()

        deletedAuthSessions.length should_== 1
        deletedAuthSessions.head.deviceTitle should_== "app1"

        val pkeys = persist.UserPublicKey.findAllByUserId(userId).sync()
        pkeys.length should_== 1

        val deletedPkeys = persist.UserPublicKey.findAllDeletedByUserId(userId).sync()
        deletedPkeys.length should_== 0
      }

      "remove previous auth and mark keyHash as deleted on sign in with the same deviceHash" in new sqlDb {
        implicit val scope = genTestScope()
        val publicKey = genPublicKey
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val pkHashes = Set(pkHash)
        val phoneNumber = genPhoneNumber()
        val name = "Timothy Klim"
        insertAuthId(scope.authId)
        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        sendRpcMsg(RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, BitVector(1), "app1", 0, "key", false))

        expectMsgByPF(withNewSession = true) {
          case RpcResponseBox(_, Ok(ResponseAuth(`pkHash`, struct.User(_, _, `name`, None, `pkHashes`, `phoneNumber`, _, _, _, None, None), _ ))) =>
        }

        Thread.sleep(2000) // let database save user

        persist.AuthSmsCode.create(phoneNumber, smsHash, smsCode).sync()

        val newPublicKey = genPublicKey
        val newPkHash = ec.PublicKey.keyHash(newPublicKey)
        val newPkHashes = Set(newPkHash)

        sendRpcMsg(RequestSignIn(phoneNumber, smsHash, smsCode, newPublicKey, BitVector(1), "app2", 0, "key"))

        var userId: Integer = null

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseAuth(`newPkHash`, struct.User(registeredUserId, _, `name`, None, `newPkHashes`, `phoneNumber`, _, _, _, None, _), _ ))) =>
            userId = registeredUserId
        }

        Thread.sleep(2000) // let database process old auth deletion

        sendRpcMsg(RequestGetAuthSessions())

        expectMsgByPF() {
          case RpcResponseBox(_, Ok(ResponseGetAuthSessions(Seq(
            struct.AuthSession(_, 0, 0, "Android Official", "app2", _, "", None, None)
          )))) =>
        }

        val authItems = persist.AuthSession.findAllByUserId(userId).sync()

        authItems.length should_== 1
        authItems.head.deviceTitle should_== "app2"

        val deletedAuthSessions = persist.AuthSession.findAllDeletedByUserId(userId).sync()

        deletedAuthSessions.length should_== 1
        deletedAuthSessions.head.deviceTitle should_== "app1"

        val pkeys = persist.UserPublicKey.findAllByUserId(userId).sync()
        pkeys.length should_== 1
        pkeys.head.hash should_== newPkHash

        val deletedPKeys = persist.UserPublicKey.findAllDeletedByUserId(userId).sync()
        deletedPKeys.length should_== 1
        deletedPKeys.head.hash should_== pkHash
      }

//      "failed with invalid public key" in new sqlDb {
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
