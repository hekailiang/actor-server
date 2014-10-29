package com.secretapp.backend.services.rpc.auth

import java.util.regex.Pattern

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.api.{ UpdatesBroker, ApiBrokerService}
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update.{ ContactRegistered, NewDevice, NewFullDevice, RemoveDevice }
import com.secretapp.backend.models
import com.secretapp.backend.helpers.SocialHelpers
import com.secretapp.backend.persist._
import com.secretapp.backend.sms.ClickatellSmsEngineActor
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import scodec.bits.BitVector
import scalaz._
import Scalaz._
import Function.tupled

trait SignService extends SocialHelpers {
  self: ApiBrokerService =>
  implicit val session: CSession

  import context._
  import UpdatesBroker._

  def handleRpcAuth: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestAuthCode =>
      unauthorizedRequest {
        handleRequestAuthCode(r.phoneNumber, r.appId, r.apiKey)
      }
    case r: RequestSignIn =>
      unauthorizedRequest {
        handleSign(
          r.phoneNumber, r.smsHash, r.smsCode, r.publicKey,
          r.deviceHash, r.deviceTitle, r.appId, r.appKey
        )(r.left)
      }
    case r: RequestSignUp =>
      unauthorizedRequest {
        handleSign(
          r.phoneNumber, r.smsHash, r.smsCode, r.publicKey,
          r.deviceHash, r.deviceTitle, r.appId, r.appKey
        )(r.right)
      }
    case r: RequestGetAuth =>
      authorizedRequest {
        handleRequestGetAuth()
      }
    case RequestRemoveAuth(id) =>
      authorizedRequest {
        handleRequestRemoveAuth(id)
      }
    case r: RequestRemoveAllOtherAuths =>
      authorizedRequest {
        handleRequestRemoveAllOtherAuths()
      }
    case r: RequestLogout =>
      authorizedRequest {
        handleRequestLogout()
      }
  }

  def handleRequestGetAuth(): Future[RpcResponse] = {
    for {
      authItems <- AuthItemRecord.getEntities(currentUser.get.uid)
    } yield {
      Ok(ResponseGetAuth(authItems.toVector map (struct.AuthItem.fromModel(_, currentUser.get.authId))))
    }
  }

  def handleRequestLogout(): Future[RpcResponse] = {
    AuthItemRecord.getEntityByUserIdAndAuthId(currentUser.get.uid, currentUser.get.authId) flatMap {
      case Some(authItem) =>
        logout(authItem) map { _ =>
          Ok(ResponseVoid())
        }
      case None =>
        Future.successful(Error(404, "USER_NOT_FOUND", "User not found", false))
    }
  }

  def handleRequestRemoveAuth(id: Int): Future[RpcResponse] = {
    AuthItemRecord.getEntity(currentUser.get.uid, id) flatMap {
      case Some(authItem) =>
        logout(authItem) map { _ =>
          Ok(ResponseVoid())
        }
      case None =>
        Future.successful(Error(404, "USER_NOT_FOUND", "User not found", false))
    }
  }

  def handleRequestRemoveAllOtherAuths(): Future[RpcResponse] = {
    AuthItemRecord.getEntities(currentUser.get.uid) map { authItems =>
      authItems foreach {
        case authItem =>
          if (authItem.authId != currentUser.get.authId) {
            logout(authItem)
          }
      }

      Ok(ResponseVoid())
    }
  }

  def handleRequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String): Future[RpcResponse] = {
    //    TODO: validate phone number
    for {
      smsR <- AuthSmsCodeRecord.getEntity(phoneNumber)
      phoneR <- PhoneRecord.getEntity(phoneNumber)
    } yield {
      val (smsHash, smsCode) = smsR match {
        case Some(models.AuthSmsCode(_, sHash, sCode)) => (sHash, sCode)
        case None =>
          val smsHash = genSmsHash
          val smsCode = phoneNumber.toString match {
            case strNumber if strNumber.startsWith("7555") =>
              strNumber { 4 }.toString * 4
            case _ => genSmsCode
          }
          AuthSmsCodeRecord.insertEntity(models.AuthSmsCode(phoneNumber, smsHash, smsCode))
          (smsHash, smsCode)
      }

      singletons.smsEngine ! ClickatellSmsEngineActor.Send(phoneNumber, smsCode) // TODO: move it to actor with persistence
      Ok(ResponseAuthCode(smsHash, phoneR.isDefined))
    }
  }

  private def handleSign(
    phoneNumber: Long, smsHash: String, smsCode: String, publicKey: BitVector,
    deviceHash: BitVector, deviceTitle: String, appId: Int, appKey: String
  )(m: RequestSignIn \/ RequestSignUp): Future[RpcResponse] = {
    val authId = currentAuthId // TODO

    @inline
    def auth(u: models.User): RpcResponse = {
      AuthSmsCodeRecord.dropEntity(phoneNumber)
      log.info(s"Authenticate currentUser=$u")
      this.currentUser = Some(u)

      nextAuthItemId() map { id =>
        val authItem = models.AuthItem.build(
          id = id, appId = appId, deviceTitle = deviceTitle, authTime = (System.currentTimeMillis / 1000).toInt,
          authLocation = "", latitude = None, longitude = None,
          authId = u.authId, deviceHash = deviceHash
        )
        log.info(s"Inserting authItem $authItem")
        AuthItemRecord.insertEntity(authItem, u.uid)
      }

      Ok(ResponseAuth(u.publicKeyHash, struct.User.fromModel(u, authId)))
    }

    @inline
    def signIn(userId: Int) = {
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)

      @inline
      def updateUserRecord(name: String): Unit = {
        UserRecord.insertPartEntityWithPhoneAndPK(userId, authId, publicKey, phoneNumber, name) onSuccess {
          case _ => pushNewDeviceUpdates(authId, userId, publicKeyHash, publicKey)
        }
      }

      @inline
      def getUserName(name: String) = m match {
        case \/-(req) => req.name
        case _ => name
      }

      for {
        userAuthR <- UserRecord.getEntity(userId, authId)
        userR <- UserRecord.getEntity(userId) // remove it when it cause bottleneck
      } yield {
        if (userR.isEmpty) Error(400, "INTERNAL_ERROR", "", true)
        else userAuthR match {
          case None =>
            val user = userR.get
            val userName = getUserName(user.name)
            updateUserRecord(userName)
            val keyHashes = user.keyHashes + publicKeyHash
            val newUser = user.copy(authId = authId, publicKey = publicKey, publicKeyHash = publicKeyHash,
              keyHashes = keyHashes, name = userName)
            auth(newUser)
          case Some(userAuth) =>
            val userName = getUserName(userAuth.name)
            if (userAuth.publicKey != publicKey) {
              //UserRecord.removeKeyHash(userId, userAuth.publicKeyHash, phoneNumber)
              updateUserRecord(userName)
              val keyHashes = userAuth.keyHashes.filter(_ != userAuth.publicKeyHash) + publicKeyHash
              val newUser = userAuth.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes,
                name = userName)
              auth(newUser)
            } else {
              if (userAuth.name != userName) {
                UserRecord.updateName(userAuth.uid, userName)
                auth(userAuth.copy(name = userName))
              } else auth(userAuth)
            }
        }
      }
    }

    def nonEmptyString(s: String): \/[NonEmptyList[String], String] = {
      val trimmed = s.trim
      if (trimmed.isEmpty) "Should be nonempty".wrapNel.left else trimmed.right
    }

    def printableString(s: String): \/[NonEmptyList[String], String] = {
      val p = Pattern.compile("\\p{Print}+", Pattern.UNICODE_CHARACTER_CLASS)
      if (p.matcher(s).matches) s.right else "Should contain printable characters only".wrapNel.left
    }

    def validName(n: String): \/[NonEmptyList[String], String] =
      nonEmptyString(n).flatMap(printableString)

    def validPublicKey(k: BitVector): \/[NonEmptyList[String], BitVector] =
      if (k == BitVector.empty) "Should be nonempty".wrapNel.left else k.right

    def validationFailed(errorName: String, errors: NonEmptyList[String]): Future[RpcResponse] =
      Future.successful(Error(400, errorName, errors.toList.mkString(", "), false))

    def withValidName(n: String)
                     (f: String => Future[RpcResponse]): Future[RpcResponse] =
      validName(n).fold(validationFailed("NAME_INVALID", _), f)

    def withValidPublicKey(k: BitVector)
                          (f: BitVector => Future[RpcResponse]): Future[RpcResponse] =
      validPublicKey(k).fold(validationFailed("PUBLIC_KEY_INVALID", _), f)

    if (smsCode.isEmpty) Future.successful(Error(400, "PHONE_CODE_EMPTY", "", false))
    else if (publicKey.length == 0) Future.successful(Error(400, "INVALID_KEY", "", false))
    else {
      val f = for {
        smsCodeR <- AuthSmsCodeRecord.getEntity(phoneNumber)
        phoneR <- PhoneRecord.getEntity(phoneNumber)
      } yield (smsCodeR, phoneR)
      f flatMap tupled {
        (smsCodeR, phoneR) =>
          if (smsCodeR.isEmpty) Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false))
          else smsCodeR.get match {
            case s if s.smsHash != smsHash => Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false))
            case s if s.smsCode != smsCode => Future.successful(Error(400, "PHONE_CODE_INVALID", "", false))
            case _ =>
              m match {
                case -\/(_: RequestSignIn) => phoneR match {
                  case None => Future.successful(Error(400, "PHONE_NUMBER_UNOCCUPIED", "", false))
                  case Some(rec) => signIn(rec.userId) // user must be persisted before sign in
                }
                case \/-(req: RequestSignUp) =>
                  AuthSmsCodeRecord.dropEntity(phoneNumber)
                  phoneR match {
                    case None => withValidName(req.name) { name =>
                      withValidPublicKey(publicKey) { publicKey =>
                        ask(clusterProxies.usersCounterProxy, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType] map { userId =>
                          val accessSalt = genUserAccessSalt
                          val user = models.User.build(userId, authId, publicKey, phoneNumber, accessSalt, name)
                          UserRecord.insertEntityWithPhoneAndPK(user)
                          pushContactRegisteredUpdates(user)
                          auth(user)
                        }
                      }
                    }
                    case Some(rec) =>
                      signIn(rec.userId)
                  }
              }
          }
      }
    }
  }

  private def nextAuthItemId(): Future[Int] = {
    ask(clusterProxies.authItemsCounterProxy, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType] andThen {
      case Failure(e) =>
        log.error("Failed to get next auth item id")
        throw e
    }
  }

  private def pushRemoveDeviceUpdates(userId: Int, publicKeyHash: Long): Unit = {
    getRelations(userId) onComplete {
      case Success(userIds) =>
        log.debug(s"Got relations for ${userId} -> ${userIds}")
        for (targetUserId <- userIds) {
          getAuthIds(targetUserId) onComplete {
            case Success(authIds) =>
              log.debug(s"Fetched authIds for userId=${targetUserId} ${authIds}")
              for (targetAuthId <- authIds) {
                updatesBrokerRegion ! NewUpdatePush(targetAuthId, RemoveDevice(userId, publicKeyHash))
              }
            case Failure(e) =>
              log.error(s"Failed to get authIds for uid=${targetUserId} to push RemoveDevice update")
              throw e
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get relations to push RemoveDevice updates userId=${userId} ${publicKeyHash}")
        throw e
    }
  }

  private def pushNewDeviceUpdates(authId: Long, userId: Int, publicKeyHash: Long, publicKey: BitVector): Unit = {
    // Push NewFullDevice updates
    UserPublicKeyRecord.fetchAuthIdsByUserId(userId) onComplete {
      case Success(authIds) =>
        log.debug(s"Fetched authIds for uid=$userId $authIds")
        for (targetAuthId <- authIds) {
          if (targetAuthId != authId) {
            log.debug(s"Pushing NewFullDevice for authId=$targetAuthId")
            updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewFullDevice(userId, publicKeyHash, publicKey))
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get authIds for authId=$authId uid=$userId to push NewFullDevice updates")
        throw e
    }

    // Push NewDevice updates
    getRelations(userId) onComplete {
      case Success(userIds) =>
        log.debug(s"Got relations for ${userId} -> ${userIds}")
        for (targetUserId <- userIds) {
          UserPublicKeyRecord.fetchAuthIdsByUserId(targetUserId) onComplete {
            case Success(authIds) =>
              log.debug(s"Fetched authIds for uid=${targetUserId} ${authIds}")
              for (targetAuthId <- authIds) {
                updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewDevice(userId, publicKeyHash))
              }
            case Failure(e) =>
              log.error(s"Failed to get authIds for authId=${authId} uid=${targetUserId} to push new device updates ${publicKeyHash}")
              throw e
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get relations to push new device updates authId=$authId uid=$userId $publicKeyHash")
        throw e
    }
  }

  private def pushContactRegisteredUpdates(u: models.User): Unit = {
    import com.secretapp.backend.api.SocialProtocol._

    UnregisteredContactRecord.byNumber(u.phoneNumber) map { contacts =>
      contacts foreach { c =>
        socialBrokerRegion ! SocialMessageBox(u.uid, RelationsNoted(Set(c.ownerUserId)))

        getAuthIds(c.ownerUserId) map { authIds =>
          authIds foreach { authId =>
            pushUpdate(authId, ContactRegistered(u.uid))
          }
        }
      }
    }
  }

  private def logout(authItem: models.AuthItem)(implicit session: CSession) = {
    UserRecord.getEntity(currentUser.get.uid, authItem.authId) map {
      case Some(user) =>
        UserRecord.removeKeyHash(user.uid, user.publicKeyHash) flatMap { _ =>
          AuthIdRecord.deleteEntity(authItem.authId) flatMap { _ =>
            AuthItemRecord.deleteEntity(user.uid, authItem.id) andThen {
              case Success(_) =>
                pushRemoveDeviceUpdates(user.uid, user.publicKeyHash)
            }
          }
        }
      case None =>
        throw new Exception("User not found")
    }
  }
}
