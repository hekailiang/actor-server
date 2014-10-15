package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.{ GroupChat, User }
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.persist.{ UserPublicKeyRecord, UserRecord, GroupChatRecord, GroupChatUserRecord, SeqUpdateRecord }
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.common.RandomService
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{ Failure, Success }
import scalaz.Scalaz._
import scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait MessagingService extends RandomService with UserHelpers {
  self: MessagingServiceActor =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  protected def handleRequestSendMessage(destUserId: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedRSAMessage
  ): Future[RpcResponse] = {
    def mkUpdates(): Future[Seq[Error \/ NewUpdateEvent]] = {
      val keysWithUserIds = (message.keys map ((destUserId, _))) ++ (message.ownKeys map ((currentUser.uid, _)))

      val futures = (keysWithUserIds) map {
        case (uid, encryptedAESKey) =>
          authIdFor(uid, encryptedAESKey.keyHash) map {
            case Some(authId) =>
              NewUpdateEvent(
                authId,
                NewMessage(
                  currentUser.uid,
                  destUserId,
                  EncryptedRSAPackage(
                    encryptedAESKey.keyHash,
                    encryptedAESKey.aesEncryptedKey,
                    message.encryptedMessage
                  )
                )
              ).right
            case None =>
              Error(404, "USER_NOT_FOUND", s"User $uid with ${encryptedAESKey.keyHash} not found", false).left
          }
      }

      Future.sequence(futures)
    }

    UserRecord.getEntity(destUserId) flatMap {
      case Some(destUserEntity) =>
        if (destUserEntity.accessHash(currentUser.authId) != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else {
          // Record relation between receiver authId and sender uid
          log.debug(s"Recording relation uid=${destUserId} -> uid=${currentUser.uid}")
          socialBrokerRegion ! SocialProtocol.SocialMessageBox(
            currentUser.uid, SocialProtocol.RelationsNoted(Set(destUserId)))

          mkUpdates() map (_.toVector) map (_.sequenceU) map {
            case -\/(error) => error
            case \/-(updates) =>
              updates foreach { update =>
                updatesBrokerRegion ! update
              }
          }

          // FIXME: handle failures (retry or error, should not break seq)
          for {
            s <- ask(
              updatesBrokerRegion,
              NewUpdateEvent(
                currentUser.authId,
                NewMessageSent(destUserId, randomId)
              )).mapTo[UpdatesBroker.StrictState]
          } yield {
            val rsp = updateProto.ResponseSeq(seq = s._1, state = Some(s._2))
            Ok(rsp)
          }
        }
      case None =>
       Future.successful(Error(400, "INTERNAL_ERROR", "Destination user not found", true))
    }
  }

  def mkInvites(
    chat: GroupChat,
    keys: EncryptedUserAESKeys,
    encryptedMessage: BitVector,
    chatUserIds: immutable.Seq[Int]
    ): Future[Seq[Error \/ NewUpdatePush]] = {
      val futures = keys.keys map { encryptedAESKey =>
        UserPublicKeyRecord.getAuthIdAndSalt(keys.userId, encryptedAESKey.keyHash) flatMap {
          case Some((authId, accessSalt)) =>
            if (User.getAccessHash(currentUser.authId, keys.userId, accessSalt) != keys.accessHash) {
              Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false).left)
            } else {
              for {
                users <- Future.sequence(chatUserIds map (getUserIdStruct(_, authId))) map (_.flatten)
              } yield {
                NewUpdatePush(
                  authId,
                  GroupInvite(
                    chat.id,
                    chat.accessHash,
                    chat.creatorUserId,
                    chat.title,
                    users,
                    EncryptedRSAPackage(
                      keyHash = encryptedAESKey.keyHash,
                      aesEncryptedKey = encryptedAESKey.aesEncryptedKey,
                      message = encryptedMessage
                    )
                  )
                ).right
              }
            }
          case None =>
            Future.successful(
              Error(404, "USER_NOT_FOUND", s"User ${keys.userId} with ${encryptedAESKey.keyHash} not found", false).left
            )
        }
      }

      Future.sequence(futures)
    }

  // TODO: don't allow to add with the same keyhash user twice
  protected def handleRequestInviteUser(
    chatId: Int, accessHash: Long, randomId: Long, chatKeyHash: BitVector, broadcast: EncryptedRSABroadcast
  ): Future[RpcResponse] = {
    val fchatUserIds = GroupChatUserRecord.getUsers(chatId)
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (chat.keyHash != chatKeyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid chat key hash.", false))
        } else {
          val f = for {
            chatUserIds <- fchatUserIds
            chatAuthIds <- Future.sequence(chatUserIds map getAuthIds) map (_.flatten)
            einvites <- Future.sequence(broadcast.keys map (mkInvites(chat, _, broadcast.encryptedMessage, chatUserIds.toVector))) map (_.flatten)
          } yield {
            (einvites, chatAuthIds)
          }

          f flatMap {
            case (einvites, chatAuthIds) =>
              einvites.toVector.sequenceU match {
                case -\/(e) => Future.successful(e)
                case \/-(updates) =>
                  val newUserIds = broadcast.keys map (key => (key.userId, key.keys map (_.keyHash) toSet))

                  Future.sequence(newUserIds map {
                    case (userId, keyHashes) =>
                      GroupChatUserRecord.addUser(chatId, userId, keyHashes) map {
                        case -\/(_) => userId.left
                        case \/-(_) => userId.right
                      }
                  }) map (_.toVector.separate) flatMap {
                    case (_, addedUsers) =>
                      updates foreach { update =>
                        updatesBrokerRegion ! update
                      }

                      chatAuthIds foreach {
                        case currentUser.authId =>
                        case authId =>
                          addedUsers foreach { userId =>
                            updatesBrokerRegion ! NewUpdatePush(authId, GroupUserAdded(chatId, userId, currentUser.uid))
                          }
                      }

                      for {
                        xs <- Future.sequence(addedUsers map { userId =>
                          ask(updatesBrokerRegion, NewUpdatePush(currentUser.authId, GroupUserAdded(chatId, userId, currentUser.uid))).mapTo[StrictState] map {
                            case (seq, state) => (seq, Some(state))
                          }
                        }) flatMap {
                          case xs if xs.isEmpty =>
                            getState(currentUser.authId) map (Seq(_))
                          case xs => Future.successful(xs)
                        }
                      } yield {
                        val s = xs.maxBy(_._1)
                        Ok(updateProto.ResponseSeq(s._1, s._2))
                      }
                  }
              }
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
    broadcast: EncryptedRSABroadcast
  ): Future[RpcResponse] = {
    val id = rand.nextInt
    val chat = GroupChat(id, currentUser.uid, rand.nextLong, title, keyHash, publicKey)

    val newUserIds = broadcast.keys map (key => (key.userId, key.keys map (_.keyHash) toSet))

    GroupChatRecord.insertEntity(chat) flatMap { _ =>
      Future.sequence(broadcast.keys map (mkInvites(chat, _, broadcast.encryptedMessage, (newUserIds map (_._1)) :+ currentUser.uid))) map (_.flatten) flatMap { einvites =>
        einvites.toVector.sequenceU match {
          case -\/(e) => Future.successful(e)
          case \/-(updates) =>
            Future.sequence(newUserIds map {
              case (userId, keyHashes) =>
                GroupChatUserRecord.addUser(chat.id, userId, keyHashes)
            }) flatMap (_ => GroupChatUserRecord.addUser(chat.id, currentUser.uid, broadcast.ownKeys map (_.keyHash) toSet)) flatMap { _ =>
              Future.sequence(broadcast.ownKeys map { key =>
                authIdFor(currentUser.uid, key.keyHash) map {
                  case Some(authId) =>
                    NewUpdatePush(
                      authId,
                      GroupCreated(
                        chatId = chat.id,
                        accessHash = chat.accessHash,
                        title = chat.title,
                        invite = EncryptedRSAPackage (
                          keyHash = key.keyHash,
                          aesEncryptedKey = key.aesEncryptedKey,
                          message = broadcast.encryptedMessage
                        )
                      )
                    ).right
                  case None =>
                    Error(404, "OWN_KEY_HASH_NOT_FOUND", s"", false).left
                }
              }) map (_.toVector.sequenceU) flatMap {
                case -\/(e) =>
                  Future.successful(e)
                case \/-(ownUpdates) =>
                  (updates ++ ownUpdates) map (updatesBrokerRegion ! _)

                  for {
                    s <- getState(currentUser.authId)
                  } yield {
                    Ok(ResponseCreateChat(chat.id, chat.accessHash, s._1, s._2))
                  }
              }
            }
        }
      }
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

              if (checkUser.accessHash(currentUser.authId) != userAccessHash) {
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
            (chatUserIds :+ currentUser.uid) foreach { userId =>
              for {
                authIds <- getAuthIds(userId)
              } yield {
                authIds foreach {
                  case currentUser.authId =>
                  case authId =>
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
    message: EncryptedAESMessage
  ): Future[RpcResponse] = {

    val fchatUserIds = GroupChatUserRecord.getUsers(chatId)
    GroupChatRecord.getEntity(chatId) flatMap { optChat =>
      optChat map { chat =>
        if (chat.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (chat.keyHash != message.keyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid chat key hash.", false))
        } else {
          fchatUserIds flatMap { userIds =>
            if (userIds.contains(currentUser.uid)) {
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
                            EncryptedAESPackage (
                              keyHash = message.keyHash,
                              message = message.encryptedMessage
                            )
                          )
                        )
                    }
                }
              }

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
            } else {
              Future.successful(Error(403, "NO_PERMISSION", "You are not a member of this group.", true))
            }
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_CHAT_DOES_NOT_EXISTS", "Group chat does not exists.", true))
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
