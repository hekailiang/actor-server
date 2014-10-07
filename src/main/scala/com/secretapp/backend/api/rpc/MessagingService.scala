package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse }
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.{ GroupChat, User }
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.persist.{ UserPublicKeyRecord, UserRecord, GroupChatRecord, GroupChatUserRecord, SeqUpdateRecord }
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.common.RandomService
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{ Failure, Success }
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingServiceActor(val updatesBrokerRegion: ActorRef, val socialBrokerRegion: ActorRef, val currentUser: User)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService with UserHelpers {
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

    case RpcProtocol.Request(RequestRemoveUser(chatId, accessHash, userId, userAccessHash)) =>
      val replyTo = sender()
      handleRequestRemoveUser(chatId, accessHash, userId, userAccessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestSendGroupMessage(chatId, accessHash, randomId, keyHash, message)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          randomIds.put(randomId, true)
          val f = handleRequestSendGroupMessage(chatId, accessHash, randomId, keyHash, message) map { res =>
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

  protected def handleRequestInviteUser(
    chatId: Int, accessHash: Long, userId: Int, userAccessHash: Long, randomId: Long, chatKeyHash: BitVector, invite: immutable.Seq[EncryptedMessage]
  ): Future[RpcResponse] = {
    val fchatUserIds = GroupChatUserRecord.getUsers(chatId)
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (chat.keyHash != chatKeyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid chat key hash.", false))
        } else {
          fchatUserIds flatMap { chatUserIds =>
            createChatUserInvites(chat, chatUserIds.toVector, userId, userAccessHash, invite) flatMap {
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
        }
      } getOrElse Future.successful(Error(404, "GROUP_CHAT_DOES_NOT_EXISTS", "Group chat does not exists.", true))
    }
  }

  protected def handleRequestRemoveUser(
    chatId: Int,
    accessHash: Long,
    userId: Int,
    userAccessHash: Long
  ): Future[RpcResponse] = {
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.creatorUserId != currentUser.uid) {
          Future.successful(Error(403, "NO_PERMISSION", "You are not creator of this chat.", false))
        } else if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid chat access hash.", false))
        } else {
          getUsers(userId) flatMap {
            case users if users.isEmpty =>
              Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
            case users =>
              val (_, checkUser) = users.head

              if (checkUser.accessHash(currentUser.uid) != userAccessHash) {
                Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid user access hash.", false))
              } else {
                GroupChatUserRecord.getUsers(chatId) flatMap { chatUserIds =>
                  if (chatUserIds.contains(userId)) {
                    for {
                      _ <- GroupChatUserRecord.removeUser(chatId, userId)
                      s <- getState(currentUser.authId)
                    } yield {
                      chatUserIds foreach { chatUserId =>
                        for {
                          authIds <- getAuthIds(chatUserId)
                        } yield {
                          authIds map { authId =>
                            updatesBrokerRegion ! NewUpdatePush(authId, GroupUserKick(
                              chatId, userId, currentUser.uid
                            ))
                          }
                        }
                      }

                      Ok(updateProto.ResponseSeq(s._1, s._2))
                    }
                  } else {
                    Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "There is no participant with such userId in this chat.", false))
                  }
                }
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
                    chatId, currentUser.uid
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
      Future.sequence(invites.toVector map (inv => createChatUserInvites(chat, (invites map (_.uid)) :+ currentUser.uid, inv.uid, inv.accessHash, inv.keys))) flatMap { ei =>
        val (errors, inviteUpdates) = ei.separate
        if (errors.length > 0) {
          Future.successful(errors.head)
        } else {
          val fselfUserAdded = GroupChatUserRecord.addUser(chat.id, currentUser.uid)

          val fusersAdded = inviteUpdates.flatten map {
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
              s <- updatesBrokerRegion.ask(NewUpdatePush(currentUser.authId, GroupCreated(
                randomId = randomId, chatId = chat.id, accessHash = chat.accessHash, title = chat.title,
                keyHash = chat.keyHash, invites = invites
              ))).mapTo[StrictState]
            } yield {
              Ok(ResponseCreateChat(chat.id, chat.accessHash, s._1, Some(s._2)))
            }
          }
        }
      }
    }
  }

  // TODO: refactor this shit
  protected def createChatUserInvites(chat: GroupChat, chatUserIds: immutable.Seq[Int], userId: Int, userAccessHash: Long, keys: immutable.Seq[EncryptedMessage]): Future[Error \/ Vector[(Long, Int, GroupInvite)]] = {
    getUsers(userId) flatMap {
      case users if users.isEmpty =>
        Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left)
      case users =>
        val (_, checkUser) = users.head

        if (checkUser.accessHash(currentUser.authId) == userAccessHash) {
          val jobOpts = keys flatMap { message =>
            message.keys map ((message.message, _))
          } map {
            case (message, key) =>
              users.toMap.get(key.keyHash) map ((_, message, key))
          }

          jobOpts.toVector.sequence map { jobs =>
            val futures: Vector[Future[(Long, Int, GroupInvite)]] = jobs map {
              case (user, message, key) =>
                for {
                  chatUserIdStructs <- Future.sequence {
                    chatUserIds map { userId =>
                      for {
                        optStruct <- getUserIdStruct(userId, user.authId)
                      } yield {
                        optStruct match {
                          case Some(struct) => struct
                          case None =>
                            log.error(s"Cannot get userId struct for $userId")
                            throw new Exception(s"Cannot get userId struct for $userId")
                        }
                      }
                    }
                  }
                } yield {
                  (
                    user.authId,
                    user.uid,
                    GroupInvite(
                      chatId = chat.id,
                      accessHash = chat.accessHash,
                      title = chat.title,
                      users = chatUserIdStructs,
                      keyHash = user.publicKeyHash,
                      aesEncryptedKey = key.aesEncryptedKey,
                      message = message
                    )
                  )
                }
            }
            Future.sequence(futures) map (_.right)
          } getOrElse (Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left))
        } else {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false).left)
        }
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

  protected def handleRequestSendGroupMessage(chatId: Int,
    accessHash: Long,
    randomId: Long,
    aesKeyHash: BitVector,
    message: BitVector): Future[RpcResponse] = {

    val fchatUserIds = GroupChatUserRecord.getUsers(chatId)
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (chat.keyHash != aesKeyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid chat key hash.", false))
        } else {
          fchatUserIds flatMap { userIds =>
            val updatesFutures = userIds map { userId =>
              getUsers(userId) map {
                case users =>
                  users.toSeq map {
                    case (_, user) =>
                    (
                      user.authId,
                      GroupMessage(
                        senderUID = currentUser.uid,
                        chatId = chat.id,
                        keyHash = user.publicKeyHash,
                        aesKeyHash = aesKeyHash,
                        message = message
                      )
                    )
                  }
              }
            }
            //updatesFutures.q
            Future.sequence(updatesFutures) map { updates =>
              updates.toVector.flatten foreach {
                case (authId, update) =>
                  updatesBrokerRegion ! NewUpdatePush(authId, update)
              }
            }

            for {
              s <- getState(currentUser.authId)
            } yield {
              Ok(updateProto.ResponseSeq(s._1, s._2))
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
          currentUser.uid, SocialProtocol.RelationsNoted(Set(uid)))

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
