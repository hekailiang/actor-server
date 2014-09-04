package com.secretapp.backend.services.rpc.auth

import java.util.regex.Pattern

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.api.UpdatesBroker._
import com.secretapp.backend.crypto.ec.PublicKey
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.update.{NewDevice, NewYourDevice}
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist._
import com.secretapp.backend.services.{GeneratorService, RpcService, UserManagerService}
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.secretapp.backend.util.HandleFutureOpt.HandleResult
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaz._
import Scalaz._
import scodec.bits.BitVector

trait SignService extends PackageCommon with RpcCommon {
  self: RpcService with Actor with GeneratorService with UserManagerService =>

  import context._

  implicit val session: CSession

  private object V {

    implicit val session: CSession = self.session

    def nonEmptyPublicKey[A](a: BitVector)
                            (f: => HandleResult[Error, A]): HandleResult[Error, A] =
      if (a.isEmpty)
        Future successful Error(400, "PUBLIC_KEY_INVALID", "Should be nonempty").left
      else
        f

    def primePublicKey[A](a: BitVector)
                         (f: => HandleResult[Error, A]): HandleResult[Error, A] =
      if (!PublicKey.isPrime192v1(a))
        Future successful Error(400, "PUBLIC_KEY_INVALID", "Invalid key").left
      else
        f

    def validPublicKey[A](a: BitVector)
                         (f: => HandleResult[Error, A]): HandleResult[Error, A] =
      nonEmptyPublicKey(a) {
      primePublicKey(a)    {
        f
      }}

    def validPhoneNumber[A](p: Long)
                           (f: Long => HandleResult[Error, A]): HandleResult[Error, A] =
      if (false)
        Future successful Error(400, "PHONE_INVALID", "").left
      else
        f(p)

    def nonEmptySmsCode[A](c: String)
                          (f: String => HandleResult[Error, A]): HandleResult[Error, A] =
      if (c.trim.isEmpty)
        Future successful Error(400, "PHONE_CODE_INVALID", "Should be nonempty").left
      else
        f(c.trim)

    def smsCodeExists[A](c: String, phone: Long)
                        (f: AuthSmsCode => HandleResult[Error, A]): HandleResult[Error, A] =
      AuthSmsCodeRecord.getEntity(phone) flatMap {
        _ some {
          f
        } none {
          Future successful Error(400, "PHONE_CODE_EXPIRED", "").left
        }
      }

    def rightSmsHash[A](received: String, stored: AuthSmsCode)
                       (f: => HandleResult[Error, A]): HandleResult[Error, A] =
      if (received != stored.smsHash)
        Future successful Error(400, "PHONE_CODE_EXPIRED", "").left
      else
        f

    def rightSmsCode[A](received: String, stored: AuthSmsCode)
                       (f: => HandleResult[Error, A]): HandleResult[Error, A] =
      if (received != stored.smsCode)
        Future successful Error(400, "PHONE_CODE_INVALID", "").left
      else
        f

    def validSmsCode[A](receivedCode: String, receivedHash: String, phoneNumber: Long)
                       (f: AuthSmsCode => HandleResult[Error, A]): HandleResult[Error, A] =
      nonEmptySmsCode(receivedCode)           { smsCode =>
      smsCodeExists(smsCode, phoneNumber)     { authSmsCode =>
      rightSmsHash(receivedHash, authSmsCode) {
      rightSmsCode(smsCode, authSmsCode)      {
        f(authSmsCode)
      }}}}

    def phoneExists[A](p: Long)
                      (f: Phone => HandleResult[Error, A]): HandleResult[Error, A] =
      PhoneRecord.getEntity(p) flatMap {
        _ some {
          f
        } none {
          Future successful Error(400, "PHONE_NUMBER_UNOCCUPIED", "").left
        }
      }

    def validRequest[A](r: RequestSign)
                       (f: (RequestSign, AuthSmsCode) => HandleResult[Error, A]): HandleResult[Error, A] =
      validPublicKey(r.publicKey)            {
      validPhoneNumber(r.phoneNumber)        { pn =>
      validSmsCode(r.smsCode, r.smsHash, pn) { authSmsCode =>
        val vr = new RequestSign {
          override val phoneNumber = pn
          override val smsHash = authSmsCode.smsHash
          override val smsCode = authSmsCode.smsCode
          override val publicKey = r.publicKey
        }
        f(vr, authSmsCode)
      }}}

    def nonEmptyName[A](n: String)
                       (f: String => HandleResult[Error, A]): HandleResult[Error, A] =
      if (n.trim.isEmpty)
        Future successful Error(400, "NAME_INVALID", "Should be nonempty").left
      else
        f(n.trim)

    def printableName[A](n: String)
                        (f: => HandleResult[Error, A]): HandleResult[Error, A] = {
       val p = Pattern.compile("\\p{Print}+", Pattern.UNICODE_CHARACTER_CLASS)
       if (!p.matcher(n).matches)
         Future successful Error(400, "NAME_INVALID", "Should contain printable characters only").left
       else
         f
     }

    def userExists[A](userId: Int)
                     (f: User => HandleResult[Error, A]): HandleResult[Error, A] =
      UserRecord.getEntity(userId) flatMap {
        _ some {
          f
        } none {
          Future successful internalError.left
        }
      }

    def validName[A](n: String)
                    (f: String => HandleResult[Error, A]): HandleResult[Error, A] =
      nonEmptyName(n)   { vn =>
      printableName(vn) {
        f(vn)
      }
      }

    def validSignInRequest[A](r: RequestSignIn)
                             (f: (RequestSignIn, AuthSmsCode, Phone) => HandleResult[Error, A]): HandleResult[Error, A] =
      validRequest(r)               { case (vr, authSmsCode) =>
      phoneExists(vr.phoneNumber) { phone =>
        f(RequestSignIn(vr.phoneNumber, vr.smsHash, vr.smsCode, vr.publicKey), authSmsCode, phone)
      }}

    def validSignUpRequest[A](r: RequestSignUp)
                             (f: (RequestSignUp, AuthSmsCode) => HandleResult[Error, A]): HandleResult[Error, A] =
      validRequest(r)   { case (vr, authSmsCode) =>
      validName(r.name) { vn =>
        f(RequestSignUp(vr.phoneNumber, vr.smsHash, vr.smsCode, vn, vr.publicKey), authSmsCode)
      }}
  }

  // TODO: use singleton for share config env
  private val clickatell = new ClickatellSMSEngine(ConfigFactory.load())

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Unit] = {
    case r: RequestAuthCode => sendRpcResult(p, messageId)(handleRequestAuthCode(r))
    case r: RequestSignIn   => sendRpcResult(p, messageId)(handleSignIn(p, r))
    case r: RequestSignUp   => sendRpcResult(p, messageId)(handleSignUp(p, r))
  }

  private def handleRequestAuthCode(r: RequestAuthCode): RpcResult =
    for {
      optSms   <- AuthSmsCodeRecord.getEntity(r.phoneNumber)
      optPhone <- PhoneRecord.getEntity(r.phoneNumber)
    } yield {
      val (smsHash, smsCode) = optSms some {
        case AuthSmsCode(_, hash, code) => (hash, code)
      } none {
        val smsHash = genSmsHash
        val strPhone = r.phoneNumber.toString
        val smsCode = if (strPhone.startsWith("7555")) strPhone(4).toString * 4 else genSmsCode

        AuthSmsCodeRecord.insertEntity(AuthSmsCode(r.phoneNumber, smsHash, smsCode))
        (smsHash, smsCode)
      }

      // TODO: move it to actor with persistence
      clickatell.send(r.phoneNumber.toString, s"Your secret app activation code: $smsCode")
      ResponseAuthCode(smsHash, optPhone.isDefined).right
    }

  private def auth(u: User, authId: Long, phone: Long): RpcResult = {
    log.info(s"Authenticate currentUser=$u")

    AuthSmsCodeRecord.dropEntity(phone) map { _ =>
      this.currentUser = u.some
      ResponseAuth(u.publicKeyHash, u.toStruct(authId)).right
    }
  }

  private def signIn(authId: Long, userId: Int, publicKey: BitVector, phone: Long): RpcResult =
    V.userExists(userId) { user =>
      def updateUserRecord(u: User) =
        UserRecord.insertPartEntityWithPhoneAndPK(u.uid, authId, publicKey, phone)

      val publicKeyHash = PublicKey.keyHash(publicKey)

      val userToAuth = user match {
        case u if u.authId == authId && u.publicKey == publicKey => u

        case u if u.authId == authId =>
          UserRecord.removeKeyHash(u.uid, u.publicKeyHash, phone)
          updateUserRecord(u) onSuccess { case _ =>
            pushNewDeviceUpdates(authId, u.uid, publicKeyHash, publicKey)
          }
          val keyHashes = u.keyHashes.filter(_ != u.publicKeyHash) + publicKeyHash
          u.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)

        case u =>
          updateUserRecord(u) onSuccess { case _ =>
            pushNewDeviceUpdates(authId, u.uid, publicKeyHash, publicKey)
          }
          val keyHashes = u.keyHashes + publicKeyHash
          u.copy(authId = authId, publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
      }

      auth(userToAuth, authId, phone)
    }

  private def handleSignIn(p: Package, r: RequestSignIn): RpcResult =
    V.validSignInRequest(r) { case (r, authSmsCode, phone) =>
      signIn(p.authId, phone.userId, r.publicKey, r.phoneNumber)
    }

  private def handleSignUp(p: Package, r: RequestSignUp): RpcResult =
    V.validSignUpRequest(r) { case (r, authSmsCode) =>
      PhoneRecord.getEntity(r.phoneNumber) flatMap {
        _ some { phone =>
          AuthSmsCodeRecord.dropEntity(r.phoneNumber)
          signIn(p.authId, phone.userId, r.publicKey, r.phoneNumber)
        } none {
          AuthSmsCodeRecord.dropEntity(r.phoneNumber)
          val userId = genUserId
          val user = User.build(userId, p.authId, r.publicKey, r.phoneNumber, genUserAccessSalt, r.name)
          UserRecord.insertEntityWithPhoneAndPK(user)
          auth(user, p.authId, r.phoneNumber)
        }
      }
    }

  private def pushNewDeviceUpdates(authId: Long, uid: Int, publicKeyHash: Long, publicKey: BitVector): Unit = {
    import com.secretapp.backend.api.SocialProtocol._

    // Push NewYourDevice updates
    UserPublicKeyRecord.fetchAuthIdsByUid(uid) onComplete {
      case Success(authIds) =>
        log.debug(s"Fetched authIds for uid=${uid} ${authIds}")
        for (targetAuthId <- authIds) {
          if (targetAuthId != authId) {
            log.debug(s"Pushing NewYourDevice for authId=${targetAuthId}")
            updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewYourDevice(uid, publicKeyHash, publicKey))
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get authIds for authId=${authId} uid=${uid} to push NewYourDevice updates")
        throw e
    }

    // Push NewDevice updates
    ask(socialBrokerRegion, SocialMessageBox(uid, GetRelations))(5.seconds).mapTo[SocialProtocol.RelationsType] onComplete {
      case Success(uids) =>
        log.debug(s"Got relations for ${uid} -> ${uids}")
        for (targetUid <- uids) {
          UserPublicKeyRecord.fetchAuthIdsByUid(targetUid) onComplete {
            case Success(authIds) =>
              log.debug(s"Fetched authIds for uid=${targetUid} ${authIds}")
              for (targetAuthId <- authIds) {
                updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewDevice(uid, publicKeyHash))
              }
            case Failure(e) =>
              log.error(s"Failed to get authIds for authId=${authId} uid=${targetUid} to push new device updates ${publicKeyHash}")
              throw e
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get relations to push new device updates authId=${authId} uid=${uid} ${publicKeyHash}")
        throw e
    }
  }
}
