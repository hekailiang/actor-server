package com.secretapp.backend.services.rpc.auth

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.api.{ UpdatesBroker, ApiBrokerService, PhoneNumber}
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update.{ NewDevice, RemovedDevice }
import com.secretapp.backend.data.message.update.contact.ContactRegistered
import com.secretapp.backend.helpers.{ ContactHelpers, SocialHelpers }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.session.SessionProtocol
import com.secretapp.backend.sms.SmsEnginesProtocol
import org.joda.time.DateTime
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import scodec.bits.BitVector
import scalaz._
import Scalaz._
import shapeless._
import Function.tupled
import com.secretapp.backend.api.rpc.RpcValidators._

trait SignService extends ContactHelpers with SocialHelpers {
  self: ApiBrokerService =>
  implicit val session: CSession

  import context._
  import UpdatesBroker._

  def handleRpcAuth: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestSendAuthCode =>
      unauthorizedRequest {
        handleRequestSendAuthCode(r.phoneNumber, r.appId, r.apiKey)
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
    case r: RequestGetAuthSessions =>
      authorizedRequest {
        handleRequestGetAuthSessions()
      }
    case RequestTerminateSession(id) =>
      authorizedRequest {
        handleRequestTerminateSession(id)
      }
    case r: RequestTerminateAllSessions =>
      authorizedRequest {
        handleRequestTerminateAllSessions()
      }
    case r: RequestSignOut =>
      authorizedRequest {
        handleRequestSignOut()
      }
  }

  def handleRequestGetAuthSessions(): Future[RpcResponse] = {
    for {
      authItems <- persist.AuthSession.findAllByUserId(currentUser.get.uid)
    } yield {
      Ok(ResponseGetAuthSessions(authItems.toVector map (struct.AuthSession.fromModel(_, currentUser.get.authId))))
    }
  }

  def handleRequestSignOut(): Future[RpcResponse] = {
    persist.AuthSession.findByUserIdAndPublicKeyHash(currentUser.get.uid, currentUser.get.publicKeyHash) flatMap {
      case Some(authItem) =>
        logout(authItem, currentUser.get) map { _ =>
          Ok(ResponseVoid())
        }
      case None =>
        Future.successful(Error(404, "USER_NOT_FOUND", "User not found", false))
    }
  }

  def handleRequestTerminateSession(id: Int): Future[RpcResponse] = {
    persist.AuthSession.findByUserIdAndId(currentUser.get.uid, id) flatMap {
      case Some(authItem) =>
        logout(authItem, currentUser.get) map { _ =>
          Ok(ResponseVoid())
        }
      case None =>
        Future.successful(Error(404, "USER_NOT_FOUND", "User not found", false))
    }
  }

  def handleRequestTerminateAllSessions(): Future[RpcResponse] = {
    persist.AuthSession.findAllByUserId(currentUser.get.uid) map { authItems =>
      authItems foreach {
        case authItem =>
          if (authItem.authId != currentUser.get.authId) {
            logout(authItem, currentUser.get)
          }
      }

      Ok(ResponseVoid())
    }
  }

  def handleRequestSendAuthCode(phoneNumberRaw: Long, appId: Int, apiKey: String): Future[RpcResponse] = {
    PhoneNumber.normalizeLong(phoneNumberRaw) match {
      case None =>
        Future.successful(Error(400, "PHONE_NUMBER_INVALID", "", true))
      case Some(phoneNumber) =>
        val smsPhoneTupleFuture = for {
          smsR <- persist.AuthSmsCode.findByPhoneNumber(phoneNumber)
          phoneR <- persist.UserPhone.findByNumber(phoneNumber)
        } yield (smsR, phoneR)
        smsPhoneTupleFuture flatMap { case (smsR, phoneR) =>
          smsR match {
            case Some(models.AuthSmsCode(_, sHash, _)) =>
              Future.successful(Ok(ResponseSendAuthCode(sHash, phoneR.isDefined)))
            case None =>
              val smsHash = genSmsHash
              val smsCode = phoneNumber.toString match {
                case strNumber if strNumber.startsWith("7555") => strNumber(4).toString * 4
                case _ => genSmsCode
              }
              singletons.smsEngines ! SmsEnginesProtocol.Send(phoneNumber, smsCode) // TODO: move it to actor with persistence
              for { _ <- persist.AuthSmsCode.create(phoneNumber = phoneNumber, smsHash = smsHash, smsCode = smsCode) }
              yield Ok(ResponseSendAuthCode(smsHash, phoneR.isDefined))
          }
        }
    }
  }

  private def handleSign(
    phoneNumberRaw: Long, smsHash: String, smsCode: String, publicKey: BitVector,
    deviceHash: BitVector, deviceTitle: String, appId: Int, appKey: String
  )(m: RequestSignIn \/ RequestSignUp): Future[RpcResponse] = {
    val authId = currentAuthId // TODO
    PhoneNumber.normalizeWithCountry(phoneNumberRaw) match {
      case None =>
        Future.successful(Error(400, "PHONE_NUMBER_INVALID", "", true))
      case Some((phoneNumber, countryCode)) =>
        @inline
        def auth(u: models.User): Future[RpcResponse] = {
          persist.AuthSmsCode.destroy(phoneNumber)
          log.info(s"Authenticate currentUser=$u")
          this.currentUser = Some(u)

          persist.AuthSession.findAllByUserIdAndDeviceHash(u.uid, deviceHash) flatMap { authItems =>
            for (authItem <- authItems) {
              logoutKeepingCurrentAuthIdAndPK(authItem, currentUser.get)
            }

            persist.AuthSession.create(
              userId = u.uid,
              id = rand.nextInt(java.lang.Integer.MAX_VALUE),
              appId = appId,
              appTitle = models.AuthSession.appTitleOf(appId),
              authId = u.authId,
              publicKeyHash = u.publicKeyHash,
              deviceHash = deviceHash,
              deviceTitle = deviceTitle,
              authTime = DateTime.now,
              authLocation = "",
              latitude = None,
              longitude = None
            )

            sessionActor ! SessionProtocol.AuthorizeUser(u)

            for {
              avatarData <- persist.AvatarData.find[models.User](u.uid)
            } yield {
              Ok(
                ResponseAuth(
                  u.publicKeyHash,
                  struct.User.fromModel(u, avatarData getOrElse(models.AvatarData.empty), authId), struct.Config(300)
                )
              )
            }
          }
        }

        @inline
        def signIn(userId: Int): Future[RpcResponse] = {
          val publicKeyHash = ec.PublicKey.keyHash(publicKey)

          @inline
          def updateUserRecord(name: String): Future[Unit] = {
            persist.User.savePartial(
              id = userId,
              name = name,
              countryCode = countryCode
            )(
              authId = authId,
              publicKeyHash = publicKeyHash,
              publicKeyData = publicKey,
              phoneNumber = phoneNumber
            ) andThen {
              case Success(_) => pushNewDeviceUpdates(authId, userId, publicKeyHash, publicKey)
            }
          }

          @inline
          def getUserName(name: String) = m match {
            case \/-(req) => req.name
            case _ => name
          }

          // TODO: use sequence from shapeless-contrib

          val (fuserAuthR, fuserR) = (
            persist.User.find(userId)(Some(authId)),
            persist.User.find(userId)(None) // remove it when it cause bottleneck
          )

          fuserAuthR flatMap { userAuthR =>
            fuserR flatMap { userR =>
              if (userR.isEmpty) Future.successful(Error(400, "INTERNAL_ERROR", "", true))
              else userAuthR match {
                case None =>
                  val user = userR.get
                  val userName = getUserName(user.name)
                  updateUserRecord(userName) flatMap { _ =>
                    val keyHashes = user.publicKeyHashes + publicKeyHash
                    val newUser = user.copy(authId = authId, publicKeyData = publicKey, publicKeyHash = publicKeyHash,
                      publicKeyHashes = keyHashes, name = userName)
                    auth(newUser)
                  }
                case Some(userAuth) =>
                  val userName = getUserName(userAuth.name)
                  if (userAuth.publicKeyData != publicKey) {
                    Future.sequence(Seq(
                      updateUserRecord(userName),
                      persist.UserPublicKey.destroy(userAuth.uid, userAuth.publicKeyHash)
                    )) flatMap { _ =>
                      val keyHashes = userAuth.publicKeyHashes.filter(_ != userAuth.publicKeyHash) + publicKeyHash
                      val newUser = userAuth.copy(publicKeyData = publicKey, publicKeyHash = publicKeyHash, publicKeyHashes = keyHashes,
                        name = userName)
                      auth(newUser)
                    }
                  } else {
                    if (userAuth.name != userName) {
                      persist.User.updateName(userAuth.uid, userName)
                      auth(userAuth.copy(name = userName))
                    } else auth(userAuth)
                  }
              }
            }
          }
        }

        if (smsCode.isEmpty) Future.successful(Error(400, "PHONE_CODE_EMPTY", "", false))
        else if (publicKey.length == 0) Future.successful(Error(400, "INVALID_KEY", "", false))
        else {
          val f = for {
            smsCodeR <- persist.AuthSmsCode.findByPhoneNumber(phoneNumber)
            phoneR <- persist.UserPhone.findByNumber(phoneNumber)
          } yield (smsCodeR, phoneR)
          f flatMap tupled {
            (smsCodeR, phoneR) =>
              if (smsCodeR.isEmpty) Future.successful(Error(400, "PHONE_CODE_EXPIRED", s"$phoneNumber $phoneNumberRaw", false))
              else smsCodeR.get match {
                case s if s.smsHash != smsHash => Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false))
                case s if s.smsCode != smsCode => Future.successful(Error(400, "PHONE_CODE_INVALID", "", false))
                case _ =>
                  m match {
                    case -\/(_: RequestSignIn) => phoneR match {
                      case None => Future.successful(Error(400, "PHONE_NUMBER_UNOCCUPIED", "", false))
                      case Some(rec) =>
                        persist.AuthSmsCode.destroy(phoneNumber)
                        signIn(rec.userId) // user must be persisted before sign in
                    }
                    case \/-(req: RequestSignUp) =>
                      persist.AuthSmsCode.destroy(phoneNumber)
                      phoneR match {
                        case None => withValidName(req.name) { name =>
                          withValidPublicKey(publicKey) { publicKey =>
                            val userId = rand.nextInt(java.lang.Integer.MAX_VALUE) + 1
                            val phoneId = rand.nextInt(java.lang.Integer.MAX_VALUE) + 1

                            persist.UserPhone.create(
                              id = phoneId,
                              userId = userId,
                              accessSalt = genAccessSalt,
                              number = phoneNumber,
                              title = "Mobile phone"
                            ) flatMap { _ =>
                              val pkHash = ec.PublicKey.keyHash(publicKey)
                              val user = models.User(
                                uid = userId,
                                authId = authId,
                                publicKeyData = publicKey,
                                publicKeyHash = pkHash,
                                phoneNumber = phoneNumber,
                                accessSalt = genAccessSalt,
                                name = name,
                                sex = models.NoSex,
                                countryCode = countryCode,
                                phoneIds = immutable.Set(phoneId),
                                emailIds = immutable.Set.empty,
                                state = models.UserState.Registered,
                                publicKeyHashes = immutable.Set(pkHash)
                              )

                              persist.AvatarData.create[models.User](user.uid, models.AvatarData.empty)

                              persist.User.create(
                                id = user.uid,
                                accessSalt = user.accessSalt,
                                name = user.name,
                                countryCode = user.countryCode,
                                sex = user.sex,
                                state = user.state
                              )(
                                authId = user.authId,
                                publicKeyHash = user.publicKeyHash,
                                publicKeyData = user.publicKeyData
                              ) flatMap { _ =>
                                pushContactRegisteredUpdates(user)
                                auth(user)
                              }
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
  }

  private def pushRemovedDeviceUpdates(userId: Int, publicKeyHash: Long): Unit = {
    getRelations(userId) onComplete {
      case Success(userIds) =>
        for (targetUserId <- userIds) {
          getAuthIds(targetUserId) onComplete {
            case Success(authIds) =>
              for (targetAuthId <- authIds) {
                updatesBrokerRegion ! NewUpdatePush(targetAuthId, RemovedDevice(userId, publicKeyHash))
              }
            case Failure(e) =>
              log.error(s"Failed to get authIds for uid=$targetUserId to push RemovedDevice update")
              throw e
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get relations to push RemovedDevice updates userId=$userId $publicKeyHash")
        throw e
    }
  }

  private def pushNewDeviceUpdates(authId: Long, userId: Int, publicKeyHash: Long, publicKey: BitVector): Unit = {
    // Push NewFullDevice updates
    persist.AuthId.findAllIdsByUserId(userId) onComplete {
      case Success(authIds) =>
        for (targetAuthId <- authIds) {
          if (targetAuthId != authId) {
            updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewDevice(userId, publicKeyHash, publicKey.some, System.currentTimeMillis()))
          }
        }
      case Failure(e) =>
        log.error(s"Failed to get authIds for authId=$authId uid=$userId to push NewFullDevice updates")
        throw e
    }

    // Push NewDevice updates
    getRelations(userId) onComplete {
      case Success(userIds) =>
        for (targetUserId <- userIds) {
          persist.AuthId.findAllIdsByUserId(targetUserId) onComplete {
            case Success(authIds) =>
              for (targetAuthId <- authIds) {
                updatesBrokerRegion ! NewUpdatePush(targetAuthId, NewDevice(userId, publicKeyHash, None, System.currentTimeMillis()))
              }
            case Failure(e) =>
              log.error(s"Failed to get authIds for authId=$authId uid=$targetUserId to push new device updates $publicKeyHash")
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

    persist.UnregisteredContact.byNumber(u.phoneNumber) map { contacts =>
      log.debug(s"unregistered ${u.phoneNumber} is in contacts of users: $contacts")
      contacts foreach { c =>
        socialBrokerRegion ! SocialMessageBox(u.uid, RelationsNoted(Set(c.ownerUserId)))

        addContact(c.ownerUserId, u.uid, u.phoneNumber, u.name, u.accessSalt)

        getAuthIds(c.ownerUserId) map { authIds =>
          authIds foreach { authId =>
            pushUpdate(authId, ContactRegistered(u.uid, false, System.currentTimeMillis()))
          }
        }
      }
      persist.UnregisteredContact.removeEntities(u.phoneNumber)
    }
  }

  private def logout(authItem: models.AuthSession, currentUser: models.User)(implicit session: CSession) = {
    // TODO: use sequence from shapeless-contrib after being upgraded to scala 2.11
    Future.sequence(Seq(
      persist.AuthId.destroy(authItem.authId),
      persist.UserPublicKey.destroy(currentUser.uid, authItem.publicKeyHash),
      persist.AuthSession.destroy(currentUser.uid, authItem.id)
    )) andThen {
      case Success(_) =>
        pushRemovedDeviceUpdates(currentUser.uid, authItem.publicKeyHash)
    }
  }

  private def logoutKeepingCurrentAuthIdAndPK(authItem: models.AuthSession, currentUser: models.User)(implicit session: CSession) = {
    val frmAuthId = if (currentUser.authId != authItem.authId) {
      persist.AuthId.destroy(authItem.authId)
    } else {
      Future.successful()
    }

    val frmKeyHash = if (currentUser.publicKeyHash != authItem.publicKeyHash) {
      persist.UserPublicKey.destroy(currentUser.uid, authItem.publicKeyHash)
    } else {
      Future.successful()
    }

    // TODO: use sequence from shapeless-contrib after being upgraded to scala 2.11
    Future.sequence(Seq(
      frmKeyHash,
      frmAuthId,
      persist.AuthSession.destroy(currentUser.uid, authItem.id)
    )) andThen {
      case Success(_) =>
        pushRemovedDeviceUpdates(currentUser.uid, authItem.publicKeyHash)
    }
  }
}
