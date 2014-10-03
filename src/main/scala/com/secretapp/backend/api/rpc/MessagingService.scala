package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse }
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.{ GroupChat, User }
import com.secretapp.backend.persist.{ UserPublicKeyRecord, UserRecord, GroupChatRecord, GroupChatUserRecord, SeqUpdateRecord }
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.common.RandomService
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingServiceActor(val updatesBrokerRegion: ActorRef, val socialBrokerRegion: ActorRef, val currentUser: User)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
  import context.{ system, become, dispatcher }

  implicit val timeout = Timeout(5.seconds)

  val counterId = currentUser.authId.toString

  val randomIds = new ConcurrentLinkedHashMap.Builder[Long, Boolean]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  def receive: Actor.Receive = {
    case RpcProtocol.Request(RequestMessageReceived(uid, randomId, accessHash)) =>
      val replyTo = sender()
      handleRequestMessageReceived(uid, randomId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestMessageRead(uid, randomId, accessHash)) =>
      val replyTo = sender()
      handleRequestMessageRead(uid, randomId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestCreateChat(randomId, title, keyHash, publicKey, invites)) =>
      val replyTo = sender()
      handleRequestCreateChat(randomId, title, keyHash, publicKey, invites) pipeTo replyTo

    case RpcProtocol.Request(RequestInviteUser(chatId, accessHash, userId, userAccessHash, randomId, chatKeyHash, invite)) =>
      val replyTo = sender()
      handleRequestInviteUser(chatId, accessHash, userId, userAccessHash, randomId, chatKeyHash, invite) pipeTo replyTo

    case RpcProtocol.Request(RequestLeaveChat(chatId, accessHash)) =>
      val replyTo = sender()
      handleRequestLeaveChat(chatId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestSendGroupMessage(chatId, accessHash, randomId, message, selfMessage)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          randomIds.put(randomId, true)
          val f = handleRequestSendGroupMessage(chatId, accessHash, randomId, message, selfMessage) map { res =>
            replyTo ! res
          }

          f onFailure {
            case err =>
              replyTo ! Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true)
              randomIds.remove(randomId)
              log.error(s"Failed to send message ${err}")
          }
      }

    case RpcProtocol.Request(RequestSendMessage(uid, accessHash, randomId, message, selfMessage)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          randomIds.put(randomId, true)
          val f = handleRequestSendMessage(uid, accessHash, randomId, message, selfMessage) map { res =>
            replyTo ! res
          }
          f onFailure {
            case err =>
              replyTo ! Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true)
              randomIds.remove(randomId)
              log.error(s"Failed to send message ${err}")
          }
      }
  }
}

sealed trait MessagingService extends RandomService {
  self: MessagingServiceActor =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  // Stores (userId, publicKeyHash) -> authId associations
  // TODO: migrate to ConcurrentLinkedHashMap
  val authIds = new TrieMap[(Int, Long), Future[Option[Long]]]

  // Caches userId -> accessHash associations
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Map[Long, User]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  protected def handleRequestInviteUser(
    chatId: Int, accessHash: Long, userId: Int, userAccessHash: Long, randomId: Long, chatKeyHash: BitVector, invite: immutable.Seq[EncryptedMessage]
  ): Future[RpcResponse] = {
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (chat.keyHash != chatKeyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid chat key hash.", false))
        } else {
          createChatUserInvites(chat, userId, userAccessHash, invite) flatMap {
            case -\/(error) =>
              Future.successful(error)
            case \/-(invites) =>
              val fuserAdded = GroupChatUserRecord.addUser(chatId, userId)
              val fauthIds = GroupChatUserRecord.getUsers(chatId) flatMap { userIds =>
                Future.sequence(userIds map getAuthIds) map (_.flatten)
              }

              for {
                _ <- fuserAdded
                authIds <- fauthIds
                s <- getState(currentUser.authId)
              } yield {
                invites map {
                  case (authId, uid, invite) =>
                    updatesBrokerRegion ! NewUpdatePush(authId, invite)
                }

                authIds foreach { authId =>
                  updatesBrokerRegion ! NewUpdatePush(authId, GroupUserAdded(chatId, userId))
                }


                Ok(updateProto.ResponseSeq(s._1, s._2))
              }
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_CHAT_DOES_NOT_EXISTS", "Group chat does not exists.", true))
    }
  }

  protected def handleRequestLeaveChat(
    chatId: Int,
    accessHash: Long
  ): Future[RpcResponse] = {
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else {
          for {
            _ <- GroupChatUserRecord.removeUser(chatId, currentUser.uid)
            chatUserIds <- GroupChatUserRecord.getUsers(chatId)
            s <- getState(currentUser.authId)
          } yield {
            chatUserIds foreach { userId =>
              for {
                authIds <- getAuthIds(userId)
              } yield {
                authIds map { authId =>
                  updatesBrokerRegion ! NewUpdatePush(authId, GroupUserLeave(
                    chatId, userId
                  ))
                }
              }
            }
            Ok(updateProto.ResponseSeq(s._1, s._2))
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_CHAT_DOES_NOT_EXISTS", "Group chat does not exists.", true))
    }
  }

  protected def handleRequestCreateChat(
    randomId: Long,
    title: String,
    keyHash: BitVector,
    publicKey: BitVector,
    invites: immutable.Seq[InviteUser]
  ): Future[RpcResponse] = {
    val id = rand.nextInt
    val chat = GroupChat(id, currentUser.uid, rand.nextLong, title, keyHash, publicKey)

    GroupChatRecord.insertEntity(chat) flatMap { _ =>
      Future.sequence(invites.toVector map (inv => createChatUserInvites(chat, inv.uid, inv.accessHash, inv.keys))) flatMap { ei =>
        val (errors, invites) = ei.separate
        if (errors.length > 0) {
          Future.successful(errors.head)
        } else {
          val fselfUserAdded = GroupChatUserRecord.addUser(chat.id, currentUser.uid)

          val fusersAdded = invites.flatten map {
            case (authId, uid, invite) =>
              GroupChatUserRecord.addUser(chat.id, uid) map (_ => (authId, invite))
          }

          Future.sequence(fusersAdded) flatMap { pairs =>
            pairs map {
              case (authId, invite) =>
                updatesBrokerRegion ! NewUpdatePush(authId, invite)
            }

            for {
              _ <- fselfUserAdded
              s <- getState(currentUser.authId)
            } yield {
              Ok(ResponseCreateChat(chat.id, chat.accessHash, s._1, s._2))
            }
          }
        }
      }
    }
  }

  protected def createChatUserInvites(chat: GroupChat, userId: Int, userAccessHash: Long, keys: immutable.Seq[EncryptedMessage]): Future[Error \/ Vector[(Long, Int, GroupInvite)]] = {
    getUsers(userId) map {
      case users if users.isEmpty =>
        Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left
      case users =>
        val (_, checkUser) = users.head

        if (checkUser.accessHash(currentUser.authId) == userAccessHash) {
          val jobsOpts = keys flatMap { message =>
            message.keys map ((message.message, _))
          } map {
            case (message, key) =>
              users.get(key.keyHash) map ((_, message, key))
          }

          val optInvites: Option[Vector[(Long, Int, GroupInvite)]] = jobsOpts.toVector.sequence map { jobs =>
            jobs map {
              case (user, message, key) =>
                (
                  user.authId,
                  user.uid,
                  GroupInvite(
                    chatId = chat.id,
                    accessHash = chat.accessHash,
                    title = chat.title,
                    users = immutable.Seq.empty,
                    keyHash = user.publicKeyHash,
                    aesEncryptedKey = key.aesEncryptedKey,
                    message = message
                  )
                )
            }
          }
          optInvites map (invites => invites.right) getOrElse (Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left)
        } else {
          Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false).left
        }
    }
  }

  protected def getUsers(uid: Int): Future[Map[Long, User]] = {
    Option(usersCache.get(uid)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        UserRecord.getEntities(uid) map (
          _ map {user => (user.publicKeyHash, user) } toMap
        )
    }
  }

  // FIXME: select from C* authId only for better performance
  protected def getAuthIds(uid: Int): Future[immutable.Seq[Long]] = {
    for {
      users <- getUsers(uid)
    } yield {
      users.toList map (_._2.authId)
    }
  }

  protected def handleRequestMessageReceived(uid: Int, randomId: Long, accessHash: Long): Future[RpcResponse] = {
    getUsers(uid) flatMap {
      case users if users.isEmpty =>
        Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
      case users =>
        val (_, user) = users.head

        if (user.accessHash(currentUser.authId) == accessHash) {
          users map {
            case (_, u) =>
              updatesBrokerRegion ! NewUpdatePush(u.authId, MessageReceived(currentUser.uid, randomId))
          }
          for {
            seq <- ask(updatesBrokerRegion, UpdatesBroker.GetSeq(currentUser.authId)).mapTo[Int]
          } yield Ok(ResponseVoid())
        } else {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        }
    }
  }

  // TODO: DRY
  protected def handleRequestMessageRead(uid: Int, randomId: Long, accessHash: Long): Future[RpcResponse] = {
    getUsers(uid) flatMap {
      case users if users.isEmpty =>
        Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
      case users =>
        val (_, user) = users.head

        if (user.accessHash(currentUser.authId) == accessHash) {
          users map {
            case (_, u) =>
              updatesBrokerRegion ! NewUpdatePush(u.authId, MessageRead(currentUser.uid, randomId))
          }
          for {
            seq <- ask(updatesBrokerRegion, UpdatesBroker.GetSeq(currentUser.authId)).mapTo[Int]
          } yield Ok(ResponseVoid())
        } else {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        }
    }
  }

  def createChatUserGroupMessages(chat: GroupChat, userId: Int, message: EncryptedMessage): Future[Error \/ Vector[(Long, Int, GroupMessage)]]  = {
    getUsers(userId) map {
      case users if users.isEmpty =>
        Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left
      case users =>
        val jobsOpts = message.keys map ((message.message, _)) map {
          case (message, key) =>
            users.get(key.keyHash) map ((_, message, key))
        }

        val optMessages: Option[Vector[(Long, Int, GroupMessage)]] = jobsOpts.toVector.sequence map { jobs =>
          jobs map {
            case (user, message, key) =>
              (
                user.authId,
                user.uid,
                GroupMessage(
                  senderUID = currentUser.uid,
                  chatId = chat.id,
                  keyHash = user.publicKeyHash,
                  aesKeyHash = chat.keyHash,
                  message = message,
                  aesEncryptedKey = key.aesEncryptedKey
                )
              )
          }
        }
        optMessages map (messages => messages.right) getOrElse (Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left)
      }
    }

  protected def handleRequestSendGroupMessage(chatId: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedMessage,
    selfMessage: Option[EncryptedMessage]): Future[RpcResponse] = {

    val fchatUserIds = GroupChatUserRecord.getUsers(chatId)
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else {
          fchatUserIds flatMap { userIds =>
            val fgroupMessages: Vector[Future[Error \/ Vector[(Long, Int, GroupMessage)]]] =
              userIds.toVector map (createChatUserGroupMessages(chat, _, message))

            val fselfMessages: Vector[Future[Error \/ Vector[(Long, Int, GroupMessage)]]] =
              selfMessage map (message => Vector(createChatUserGroupMessages(chat, currentUser.uid, message))) getOrElse Vector.empty

            Future.sequence(fgroupMessages ++ fselfMessages) flatMap { ei =>
              val (errors, messages) = ei.separate

              if (errors.length > 0) {
                Future.successful(errors.head)
              } else {
                messages.flatten map {
                  case (authId, uid, message) =>
                    updatesBrokerRegion ! NewUpdatePush(authId, message)
                }
                for {
                  s <- getState(currentUser.authId)
                } yield {
                  Ok(updateProto.ResponseSeq(s._1, s._2))
                }
              }
            }
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_CHAT_DOES_NOT_EXISTS", "Group chat does not exists.", true))
    }
  }

  protected def handleRequestSendMessage(uid: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedMessage,
    selfMessage: Option[EncryptedMessage]
  ): Future[RpcResponse] = {
    // TODO: check accessHash SA-21

    @inline
    def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long]] = {
      log.debug(s"Resolving authId for ${uid} ${publicKeyHash}")
      authIds.get((uid, publicKeyHash)) match {
        case Some(f) =>
          f onSuccess {
            case authId =>
              log.debug(s"Resolved(cache) authId $authId for $uid $publicKeyHash")
          }
          f
        case None =>
          val f = UserPublicKeyRecord.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
          authIds.put((uid, publicKeyHash), f)
          f onSuccess { case authId => log.debug(s"Resolved authId $authId for $uid $publicKeyHash") }
          f
      }
    }

    @inline
    def pushUpdates(): Unit = {
      val pairs = selfMessage match {
        case Some(realSelfMessage) =>
          Set(
            (uid, message),
            (currentUser.uid, realSelfMessage)
          )
        case None => Set((uid, message))
      }

      pairs map { case (targetUid, encMessage) =>
        message.keys map { key =>
          authIdFor(targetUid, key.keyHash) onComplete {
            case Success(Some(targetAuthId)) =>
              log.info(s"Pushing to authId $targetAuthId message ${encMessage}")
              updatesBrokerRegion ! NewUpdateEvent(targetAuthId, NewMessage(currentUser.uid, targetUid, key.keyHash, key.aesEncryptedKey, encMessage.message))
            case x =>
              throw new Exception(s"Cannot find authId for uid=${targetUid} publicKeyHash=${key.keyHash}")
          }
        }
      }
    }

    UserRecord.getEntity(uid) flatMap {
      case Some(destUserEntity) =>
        val updatesDestUserId = destUserEntity.uid
        val updatesDestPublicKeyHash = destUserEntity.publicKeyHash

        // Record relation between receiver authId and sender uid
        log.debug(s"Recording relation uid=${uid} -> uid=${currentUser.uid}")
        socialBrokerRegion ! SocialProtocol.SocialMessageBox(
          uid, SocialProtocol.RelationsNoted(Set(currentUser.uid)))

        pushUpdates()

        // FIXME: handle failures (retry or error, should not break seq)
        for {
          s <- ask(
            updatesBrokerRegion, NewUpdateEvent(currentUser.authId, NewMessageSent(uid, randomId))).mapTo[UpdatesBroker.StrictState]
        } yield {
          val rsp = updateProto.ResponseSeq(seq = s._1, state = Some(s._2))
          Ok(rsp)
        }
      case None =>
       Future.successful(Error(400, "INTERNAL_ERROR", "Destination user not found", true))
    }
  }

  private def getSeq(authId: Long): Future[Int] = {
    ask(updatesBrokerRegion, UpdatesBroker.GetSeq(authId)).mapTo[Int]
  }

  protected def getState(authId: Long)(implicit session: CSession): Future[(Int, Option[UUID])] = {
    for {
      seq <- getSeq(authId)
      muuid <- SeqUpdateRecord.getState(authId)
    } yield (seq, muuid)
  }
}
