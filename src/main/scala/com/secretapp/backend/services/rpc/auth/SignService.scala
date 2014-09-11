package com.secretapp.backend.services.rpc.auth

import java.util.regex.Pattern

import akka.actor._
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.crypto.ec.PublicKey
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.update.{NewDevice, NewYourDevice}
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.services.{GeneratorService, RpcService, UserManagerService}
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.typesafe.config.ConfigFactory
import scodec.bits.BitVector

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait SignService extends PackageCommon with RpcCommon {
  self: RpcService with Actor with GeneratorService with UserManagerService =>

  import context._

  implicit val session: CSession

  private object V {

    private implicit val session: CSession = self.session

    private def check[A](p: Boolean, a: A, e: Error): Result[A] =
      Future.successful(if (p) a.right else e.left)

    def nonEmptyPublicKey(a: BitVector): Result[BitVector] =
      check(!a.isEmpty, a, Error(400, "PUBLIC_KEY_INVALID", "Should be nonempty"))

    def primePublicKey(a: BitVector): Result[BitVector] =
      check(PublicKey.isPrime192v1(a), a, Error(400, "PUBLIC_KEY_INVALID", "Invalid key"))

    def validPublicKey(a: BitVector): Result[BitVector] =
      for (
        pk <- nonEmptyPublicKey(a);
        pk <- primePublicKey(a)
      ) yield pk

    def validPhoneNumber(p: Long): Result[Long] =
      check(true, p, Error(400, "PHONE_INVALID", "")) // TODO: Implement check

    def nonEmptySmsCode(c: String): Result[String] =
      check(!c.trim.isEmpty, c.trim, Error(400, "PHONE_CODE_INVALID", "Should be nonempty"))

    def smsCodeExists(c: String, phone: Long): Result[AuthSmsCode] =
      AuthSmsCodeRecord.getEntity(phone) map {
        _.fold(Error(400, "PHONE_CODE_EXPIRED", "").left[AuthSmsCode])(_.right)
      }

    def rightSmsHash(received: String, stored: AuthSmsCode): Result[AuthSmsCode] =
      check(received == stored.smsHash, stored, Error(400, "PHONE_CODE_EXPIRED", ""))

    def rightSmsCode(received: String, stored: AuthSmsCode): Result[AuthSmsCode] =
      check(received == stored.smsCode, stored, Error(400, "PHONE_CODE_INVALID", ""))

    def validSmsCode(receivedCode: String, receivedHash: String, phoneNumber: Long): Result[AuthSmsCode] =
      for (
        smsCode     <- nonEmptySmsCode(receivedCode);
        authSmsCode <- smsCodeExists(smsCode, phoneNumber);
        _           <- rightSmsHash(receivedHash, authSmsCode);
        _           <- rightSmsCode(smsCode, authSmsCode)
      ) yield authSmsCode

    def phoneExists(p: Long): Result[Phone] =
      PhoneRecord.getEntity(p) map {
        _.fold(Error(400, "PHONE_NUMBER_UNOCCUPIED", "").left[Phone])(_.right)
      }

    def validRequest(r: RequestSign): Result[(RequestSign, AuthSmsCode)] =
      for (
        pk          <- validPublicKey(r.publicKey);
        pn          <- validPhoneNumber(r.phoneNumber);
        authSmsCode <- validSmsCode(r.smsCode, r.smsHash, pn);
        vr = new RequestSign {
          override val phoneNumber = pn
          override val smsHash = authSmsCode.smsHash
          override val smsCode = authSmsCode.smsCode
          override val publicKey = r.publicKey
        }
      ) yield (vr: RequestSign, authSmsCode) // TODO: Remove type cast. It is for Idea to highlight properly.

    def nonEmptyName(n: String): Result[String] =
      check(!n.trim.isEmpty, n.trim, Error(400, "NAME_INVALID", "Should be nonempty"))

    def printableName(n: String): Result[String] = {
       val p = Pattern.compile("\\p{Print}+", Pattern.UNICODE_CHARACTER_CLASS)
       check(p.matcher(n).matches, n, Error(400, "NAME_INVALID", "Should contain printable characters only"))
     }

    def userExists(userId: Int): Result[User] =
      UserRecord.getEntity(userId) map {
        _.fold(internalError.left[User])(_.right)
      }

    def validName(n: String): Result[String] =
      for (
        n <- nonEmptyName(n);
        n <- printableName(n)
      ) yield n

    def validSignInRequest(r: RequestSignIn): Result[(RequestSignIn, AuthSmsCode, Phone)] =
      for (
        (vr, authSmsCode) <- validRequest(r);
        phone             <- phoneExists(vr.phoneNumber);
        cr = RequestSignIn(vr.phoneNumber, vr.smsHash, vr.smsCode, vr.publicKey)
      ) yield (cr, authSmsCode, phone)

    def validSignUpRequest(r: RequestSignUp): Result[(RequestSignUp, AuthSmsCode)] =
      for (
        (vr, authSmsCode) <- validRequest(r);
        vn                <- validName(r.name);
        cr = RequestSignUp(vr.phoneNumber, vr.smsHash, vr.smsCode, vn, vr.publicKey)
      ) yield (cr, authSmsCode)
  }

  // TODO: use singleton for share config env
  private val clickatell = new ClickatellSMSEngine(ConfigFactory.load())

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Unit] = {
    case r: RequestAuthCode => sendRpcResult(p, messageId)(handleRequestAuthCode(p, r))
    case r: RequestSignIn   => sendRpcResult(p, messageId)(handleSignIn(p, r))
    case r: RequestSignUp   => sendRpcResult(p, messageId)(handleSignUp(p, r))
  }

  private def handleRequestAuthCode(p: Package, r: RequestAuthCode): RpcResult =
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
    V.userExists(userId) flatMap { user =>
      val publicKeyHash = PublicKey.keyHash(publicKey)

      def updateUserRecord() =
        UserRecord.insertPartEntityWithPhoneAndPK(user.uid, authId, publicKey, phone) onSuccess { case _ =>
          pushNewDeviceUpdates(authId, user, publicKeyHash, publicKey)
        }

      val userToAuth = user match {
        case u if u.authId == authId && u.publicKey == publicKey => u

        case u if u.authId == authId =>
          UserRecord.removeKeyHash(u.uid, u.publicKeyHash, phone)
          updateUserRecord()
          val keyHashes = u.keyHashes.filter(_ != u.publicKeyHash) + publicKeyHash
          u.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)

        case u =>
          updateUserRecord()
          val keyHashes = u.keyHashes + publicKeyHash
          u.copy(authId = authId, publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
      }

      auth(userToAuth, authId, phone)
    }

  private def handleSignIn(p: Package, r: RequestSignIn): RpcResult =
    V.validSignInRequest(r) flatMap { case (r, authSmsCode, phone) =>
      signIn(p.authId, phone.userId, r.publicKey, r.phoneNumber)
    }

  private def handleSignUp(p: Package, r: RequestSignUp): RpcResult =
    V.validSignUpRequest(r) flatMap { case (r, authSmsCode) =>
      AuthSmsCodeRecord.dropEntity(r.phoneNumber)

      PhoneRecord.getEntity(r.phoneNumber) flatMap {
        _ some { phone =>
          signIn(p.authId, phone.userId, r.publicKey, r.phoneNumber)
        } none {
          val userId = genUserId
          val user = User.build(userId, p.authId, r.publicKey, r.phoneNumber, genUserAccessSalt, r.name)
          UserRecord.insertEntityWithPhoneAndPK(user)
          auth(user, p.authId, r.phoneNumber)
        }
      }
    }

  private def pushNewDeviceUpdates(authId: Long, u: User, publicKeyHash: Long, publicKey: BitVector): Unit = {
    withAuthIds(u.uid) {
      _.foreach { targetAuthId =>
        if (targetAuthId != authId) {
          log.debug(s"Pushing NewYourDevice for authId=$targetAuthId")
          pushUpdate(targetAuthId, NewYourDevice(u.uid, publicKeyHash, publicKey))
        }
      }
    }

    withRelations(u.uid) {
      _.foreach(pushUpdate(_, NewDevice(u.uid, publicKeyHash)))
    }
  }
}
