package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.struct.{ FileLocation, Avatar }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.{ Group, User }
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.persist._
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
    group: Group,
    keys: EncryptedUserAESKeys,
    encryptedMessage: BitVector,
    groupUserIds: immutable.Seq[Int]
    ): Future[Seq[Error \/ NewUpdatePush]] = {
      val futures = keys.keys map { encryptedAESKey =>
        UserPublicKeyRecord.getAuthIdAndSalt(keys.userId, encryptedAESKey.keyHash) flatMap {
          case Some((authId, accessSalt)) =>
            if (User.getAccessHash(currentUser.authId, keys.userId, accessSalt) != keys.accessHash) {
              Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false).left)
            } else {
              for {
                users <- Future.sequence(groupUserIds map (getUserIdStruct(_, authId))) map (_.flatten)
              } yield {
                NewUpdatePush(
                  authId,
                  GroupInvite(
                    group.id,
                    group.accessHash,
                    group.creatorUserId,
                    currentUser.uid,
                    group.title,
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
    groupId: Int, accessHash: Long, randomId: Long, groupKeyHash: BitVector, broadcast: EncryptedRSABroadcast
  ): Future[RpcResponse] = {
    val fgroupUserIds = GroupUserRecord.getUsers(groupId)
    GroupRecord.getEntity(groupId) flatMap { optGroup =>
      optGroup map { group =>
        if (group.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (group.keyHash != groupKeyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid group key hash.", false))
        } else {
          val f = for {
            groupUserIds <- fgroupUserIds
            groupAuthIds <- Future.sequence(groupUserIds map getAuthIds) map (_.flatten)
            einvites <- Future.sequence(broadcast.keys map (mkInvites(group, _, broadcast.encryptedMessage, groupUserIds.toVector))) map (_.flatten)
          } yield {
            (einvites, groupAuthIds)
          }

          f flatMap {
            case (einvites, groupAuthIds) =>
              einvites.toVector.sequenceU match {
                case -\/(e) => Future.successful(e)
                case \/-(updates) =>
                  val newUserIds = broadcast.keys map (key => (key.userId, key.keys map (_.keyHash) toSet))

                  Future.sequence(newUserIds map {
                    case (userId, keyHashes) =>
                      GroupUserRecord.addUser(groupId, userId, keyHashes) map {
                        case -\/(_) => userId.left
                        case \/-(_) => userId.right
                      }
                  }) map (_.toVector.separate) flatMap {
                    case (_, addedUsers) =>
                      updates foreach { update =>
                        updatesBrokerRegion ! update
                      }

                      groupAuthIds foreach {
                        case currentUser.authId =>
                        case authId =>
                          addedUsers foreach { userId =>
                            updatesBrokerRegion ! NewUpdatePush(authId, GroupUserAdded(groupId, userId, currentUser.uid))
                          }
                      }

                      for {
                        xs <- Future.sequence(addedUsers map { userId =>
                          ask(updatesBrokerRegion, NewUpdatePush(currentUser.authId, GroupUserAdded(groupId, userId, currentUser.uid))).mapTo[StrictState] map {
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
      } getOrElse Future.successful(Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true))
    }
  }

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    keyHash: BitVector,
    publicKey: BitVector,
    broadcast: EncryptedRSABroadcast
  ): Future[RpcResponse] = {
    val id = rand.nextInt
    val group = Group(id, currentUser.uid, rand.nextLong, title, keyHash, publicKey)

    val newUserIds = broadcast.keys map (key => (key.userId, key.keys map (_.keyHash) toSet))

    GroupRecord.insertEntity(group) flatMap { _ =>
      Future.sequence(broadcast.keys map (mkInvites(group, _, broadcast.encryptedMessage, (newUserIds map (_._1)) :+ currentUser.uid))) map (_.flatten) flatMap { einvites =>
        einvites.toVector.sequenceU match {
          case -\/(e) => Future.successful(e)
          case \/-(updates) =>
            Future.sequence(newUserIds map {
              case (userId, keyHashes) =>
                Future.sequence(immutable.Seq(
                  GroupUserRecord.addUser(group.id, userId, keyHashes),
                  UserGroupsRecord.addGroup(userId, group.id)
                ))
            }) flatMap { _ =>
              Future.sequence(immutable.Seq(
                GroupUserRecord.addUser(group.id, currentUser.uid, broadcast.ownKeys map (_.keyHash) toSet),
                UserGroupsRecord.addGroup(currentUser.uid, group.id)
              ))
            } flatMap { _ =>
              Future.sequence(broadcast.ownKeys map { key =>
                authIdFor(currentUser.uid, key.keyHash) flatMap {
                  case Some(authId) =>
                    for {
                      users <- Future.sequence(
                        ((newUserIds map (_._1)) :+ currentUser.uid) map { newUserId =>
                          getUserIdStruct(newUserId, authId)
                        }
                      )
                    } yield {
                      NewUpdatePush(
                        authId,
                        GroupCreated(
                          groupId = group.id,
                          accessHash = group.accessHash,
                          title = group.title,
                          invite = EncryptedRSAPackage (
                            keyHash = key.keyHash,
                            aesEncryptedKey = key.aesEncryptedKey,
                            message = broadcast.encryptedMessage
                          ),
                          users = users.flatten
                        )
                      ).right
                    }
                  case None =>
                    Future.successful(Error(404, "OWN_KEY_HASH_NOT_FOUND", s"", false).left)
                }
              }) map (_.toVector.sequenceU) flatMap {
                case -\/(e) =>
                  Future.successful(e)
                case \/-(ownUpdates) =>
                  (updates ++ ownUpdates) map (updatesBrokerRegion ! _)

                  for {
                    s <- getState(currentUser.authId)
                  } yield {
                    Ok(ResponseCreateGroup(group.id, group.accessHash, s._1, s._2))
                  }
              }
            }
        }
      }
    }
  }

  protected def handleRequestEditGroupAvatar(
    groupId: Int, accessHash: Long, avatar: FileLocation
  ): Future[RpcResponse] = ???

  protected def handleRequestEditGroupTitle(
    groupId: Int,
    accessHash: Long,
    title: String
  ): Future[RpcResponse] = {
    GroupRecord.getEntity(groupId) flatMap { optGroup =>
      optGroup map { group =>
        if (group.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid group access hash.", false))
        } else {
          for {
            _ <- GroupRecord.setTitle(groupId, title)
            s <- getState(currentUser.authId)
          } yield {
            GroupUserRecord.getUsers(groupId) onSuccess {
              case groupUserIds =>
                groupUserIds foreach { groupUserId =>
                  for {
                    authIds <- getAuthIds(groupUserId)
                  } yield {
                    authIds map { authId =>
                      updatesBrokerRegion ! NewUpdatePush(authId, GroupTitleChanged(
                        groupId, title
                      ))
                    }
                  }
                }
            }

            Ok(updateProto.ResponseSeq(s._1, s._2))
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true))
    }
  }

  protected def handleRequestRemoveUser(
    groupId: Int,
    accessHash: Long,
    userId: Int,
    userAccessHash: Long
  ): Future[RpcResponse] = {
    GroupRecord.getEntity(groupId) flatMap { optGroup =>
      optGroup map { group =>
        if (group.creatorUserId != currentUser.uid) {
          Future.successful(Error(403, "NO_PERMISSION", "You are not creator of this group.", false))
        } else if (group.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid group access hash.", false))
        } else {
          getUsers(userId) flatMap {
            case users if users.isEmpty =>
              Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
            case users =>
              val (_, checkUser) = users.head

              if (checkUser.accessHash(currentUser.authId) != userAccessHash) {
                Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid user access hash.", false))
              } else {
                GroupUserRecord.getUsers(groupId) flatMap { groupUserIds =>
                  if (groupUserIds.contains(userId)) {
                    for {
                      _ <- Future.sequence(immutable.Seq(
                        GroupUserRecord.removeUser(groupId, userId),
                        UserGroupsRecord.removeGroup(userId, groupId)
                      ))
                      s <- getState(currentUser.authId)
                    } yield {
                      groupUserIds foreach { groupUserId =>
                        for {
                          authIds <- getAuthIds(groupUserId)
                        } yield {
                          authIds map { authId =>
                            updatesBrokerRegion ! NewUpdatePush(authId, GroupUserKick(
                              groupId, userId, currentUser.uid
                            ))
                          }
                        }
                      }

                      Ok(updateProto.ResponseSeq(s._1, s._2))
                    }
                  } else {
                    Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "There is no participant with such userId in this group.", false))
                  }
                }
              }
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true))
    }
  }

  protected def handleRequestLeaveGroup(
    groupId: Int,
    accessHash: Long
  ): Future[RpcResponse] = {
    GroupRecord.getEntity(groupId) flatMap { optGroup =>
      optGroup map { group =>
        if (group.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else {
          for {
            _ <- Future.sequence(immutable.Seq(
              GroupUserRecord.removeUser(groupId, currentUser.uid),
              UserGroupsRecord.removeGroup(currentUser.uid, groupId)
            ))
            groupUserIds <- GroupUserRecord.getUsers(groupId)
            s <- getState(currentUser.authId)
          } yield {
            (groupUserIds :+ currentUser.uid) foreach { userId =>
              for {
                authIds <- getAuthIds(userId)
              } yield {
                authIds foreach {
                  case currentUser.authId =>
                  case authId =>
                    updatesBrokerRegion ! NewUpdatePush(authId, GroupUserLeave(
                      groupId, currentUser.uid
                    ))
                }
              }
            }
            Ok(updateProto.ResponseSeq(s._1, s._2))
          }
        }
      } getOrElse Future.successful(Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true))
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

  protected def handleRequestSendGroupMessage(groupId: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedAESMessage
  ): Future[RpcResponse] = {

    val fgroupUserIds = GroupUserRecord.getUsers(groupId)
    GroupRecord.getEntity(groupId) flatMap { optGroup =>
      optGroup map { group =>
        if (group.accessHash != accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else if (group.keyHash != message.keyHash) {
          Future.successful(Error(400, "WRONG_KEY", "Invalid group key hash.", false))
        } else {
          fgroupUserIds flatMap { userIds =>
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
                            groupId = group.id,
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
      } getOrElse Future.successful(Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true))
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
