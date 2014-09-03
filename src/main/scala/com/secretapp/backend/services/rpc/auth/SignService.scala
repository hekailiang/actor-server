package com.secretapp.backend.services.rpc.auth

import java.util.regex.Pattern

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.ResultSet
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.message.update.NewDevice
import com.secretapp.backend.data.message.update.NewYourDevice
import com.secretapp.backend.services.RpcService
import com.secretapp.backend.services.UserManagerService
import scala.collection.immutable
import scala.collection.immutable.Seq
import scala.util.Success
import scala.util.{ Random, Try, Success, Failure }
import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.data.message.struct.{ User => StructUser }
import com.secretapp.backend.data.message.{ TransportMessage, RpcResponseBox }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.secretapp.backend.data.transport._
import com.secretapp.backend.util.HandleFutureOpt._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.crypto.ec
import scodec.bits.BitVector
import scalaz._
import Scalaz._
import Function.tupled

trait SignService extends PackageCommon with RpcCommon {
  self: RpcService with Actor with GeneratorService with UserManagerService =>
  implicit val session: CSession

  import context._
  import UpdatesBroker._

  val serverConfig = ConfigFactory.load()
  val clickatell = new ClickatellSMSEngine(serverConfig) // TODO: use singleton for share config env

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode =>
      sendRpcResult(p, messageId)(handleRequestAuthCode(r.phoneNumber, r.appId, r.apiKey))
    case r: RequestSignIn =>
      sendRpcResult(p, messageId)(handleSign(p)(r.phoneNumber, r.smsHash, r.smsCode, r.publicKey)(r.left))
    case r: RequestSignUp =>
      sendRpcResult(p, messageId)(handleSign(p)(r.phoneNumber, r.smsHash, r.smsCode, r.publicKey)(r.right))
  }

  def handleRequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String): RpcResult = {
    //    TODO: validate phone number
    for {
      smsR <- AuthSmsCodeRecord.getEntity(phoneNumber)
      phoneR <- PhoneRecord.getEntity(phoneNumber)
    } yield {
      val (smsHash, smsCode) = smsR match {
        case Some(AuthSmsCode(_, sHash, sCode)) => (sHash, sCode)
        case None =>
          val smsHash = genSmsHash
          val smsCode = phoneNumber.toString match {
            case strNumber if strNumber.startsWith("7555") =>
              strNumber { 4 }.toString * 4
            case _ => genSmsCode
          }
          AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode))
          (smsHash, smsCode)
      }
      clickatell.send(phoneNumber.toString, s"Your secret app activation code: $smsCode") // TODO: move it to actor with persistence
      ResponseAuthCode(smsHash, phoneR.isDefined).right
    }
  }

  private def handleSign(p: Package)(phoneNumber: Long, smsHash: String, smsCode: String, publicKey: BitVector)(m: RequestSignIn \/ RequestSignUp): RpcResult = {
    val authId = p.authId // TODO

    @inline
    def auth(u: User) = {
      AuthSmsCodeRecord.dropEntity(phoneNumber)
      log.info(s"Authenticate currentUser=${u}")
      this.currentUser = Some(u)
      ResponseAuth(u.publicKeyHash, u.toStruct(authId)).right
    }

    @inline
    def signIn(userId: Int): RpcResult = {
      @inline
      def updateUserRecord(): Future[ResultSet] = m match {
        case -\/(_: RequestSignIn) =>
          UserRecord.insertPartEntityWithPhoneAndPK(userId, authId, publicKey, phoneNumber)
        case \/-(req: RequestSignUp) =>
          UserRecord.insertPartEntityWithPhoneAndPK(userId, authId, publicKey, phoneNumber, req.firstName,
            req.lastName)
      }

      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      for {
        userAuthR <- UserRecord.getEntity(userId, authId)
        userR <- UserRecord.getEntity(userId) // remove it when it cause bottleneck
      } yield {
        if (userR.isEmpty) internalError.left
        else userAuthR match {
          case None =>
            updateUserRecord() onSuccess {
              case _ =>
                pushNewDeviceUpdates(authId, userId, publicKeyHash, publicKey)
            }
            val user = userR.get
            val keyHashes = user.keyHashes + publicKeyHash
            val newUser = user.copy(authId = authId, publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
            auth(newUser)
          case Some(userAuth) =>
            if (userAuth.publicKey != publicKey) {
              UserRecord.removeKeyHash(userId, userAuth.publicKeyHash, phoneNumber)
              updateUserRecord() onSuccess {
                case _ =>
                  pushNewDeviceUpdates(authId, userId, publicKeyHash, publicKey)
              }
              val keyHashes = userAuth.keyHashes.filter(_ != userAuth.publicKeyHash) + publicKeyHash
              val newUser = userAuth.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
              auth(newUser)
            } else auth(userAuth)
        }
      }
    }

    def nonEmptyString(s: String): ValidationNel[String, String] = {
      val trimmed = s.trim
      if (trimmed.isEmpty) "Should be nonempty".failureNel else trimmed.success
    }

    def printableString(s: String): ValidationNel[String, String] = {
      val p = Pattern.compile("\\p{Print}+")
      if (p.matcher(s).matches()) s.success else "Should contain printable characters only".failureNel
    }

    def validName(n: String): ValidationNel[String, String] = {
      import scalaz.Validation.FlatMap._
      nonEmptyString(n).flatMap(printableString)
    }

    def withValidName(n: String, e: String)
                     (f: String => Future[\/[Error, RpcResponseMessage]]): Future[\/[Error, RpcResponseMessage]] =
      validName(n).fold({ errors =>
        Future.successful(Error(400, e, errors.toList.mkString(", "), false).left)
      }, f)

    def withValidOptName(optn: Option[String], e: String)
                        (f: Option[String] => Future[\/[Error, RpcResponseMessage]]): Future[\/[Error, RpcResponseMessage]] =
      optn
        .some(n => withValidName(n, e)(vn => f(vn.some)))
        .none(f(none))

    def withValidFirstName(n: String) = withValidName(n, "FIRST_NAME_INVALID") _

    def withValidOptLastName(optn: Option[String]) = withValidOptName(optn, "LAST_NAME_INVALID") _

    def validPublicKey(k: BitVector): ValidationNel[String, BitVector] =
      if (k == BitVector.empty) "Should be nonempty".failureNel else k.success

    def withValidPublicKey(k: BitVector)(f: BitVector => Future[\/[Error, RpcResponseMessage]]): Future[\/[Error, RpcResponseMessage]] =
      validPublicKey(k).fold({ errors =>
        Future.successful(Error(400, "PUBLIC_KEY_INVALID", errors.toList.mkString(", "), false).left)
      }, f)

    if (smsCode.isEmpty) Future.successful(Error(400, "PHONE_CODE_EMPTY", "", false).left)
    else if (!ec.PublicKey.isPrime192v1(publicKey)) Future.successful(Error(400, "INVALID_KEY", "", false).left)
    else {
      val f = for {
        smsCodeR <- AuthSmsCodeRecord.getEntity(phoneNumber)
        phoneR <- PhoneRecord.getEntity(phoneNumber)
      } yield (smsCodeR, phoneR)
      f flatMap tupled {
        (smsCodeR, phoneR) =>
          if (smsCodeR.isEmpty) Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false).left)
          else smsCodeR.get match {
            case s if s.smsHash != smsHash => Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false).left)
            case s if s.smsCode != smsCode => Future.successful(Error(400, "PHONE_CODE_INVALID", "", false).left)
            case _ =>
              m match {
                case -\/(_: RequestSignIn) => phoneR match {
                  case None => Future.successful(Error(400, "PHONE_NUMBER_UNOCCUPIED", "", false).left)
                  case Some(rec) => signIn(rec.userId) // user must be persisted before sign in
                }
                case \/-(req: RequestSignUp) =>
                  AuthSmsCodeRecord.dropEntity(phoneNumber)
                  phoneR match {
                    case None => withValidFirstName(req.firstName) { firstName =>
                      withValidOptLastName(req.lastName) { optLastName =>
                        withValidPublicKey(publicKey) { publicKey =>
                          val userId = genUserId
                          val accessSalt = genUserAccessSalt
                          val user = User.build(uid = userId, authId = authId, publicKey = publicKey, accessSalt = accessSalt,
                            phoneNumber = phoneNumber, firstName = firstName, lastName = optLastName)
                          UserRecord.insertEntityWithPhoneAndPK(user)
                          Future.successful(auth(user))
                        }
                      }
                    }
                    case Some(rec) => signIn(rec.userId)
                  }
              }
          }
      }
    }
  }

  private def pushNewDeviceUpdates(authId: Long, uid: Int, publicKeyHash: Long, publicKey: BitVector): Unit = {
    import SocialProtocol._

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
