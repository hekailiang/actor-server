package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseAvatarChanged, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.helpers._
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait GroupHandlers extends RandomService with UserHelpers with GroupHelpers with PeerHelpers with UpdatesHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  val handleGroup: RequestMatcher = {
    case RequestCreateGroup(randomId, title, users) =>
      handleRequestCreateGroup(randomId, title, users)
    case RequestEditGroupTitle(groupPeer, title) =>
      handleRequestEditGroupTitle(groupPeer, title)
    case RequestInviteUsers(groupOutPeer, users) =>
      handleRequestInviteUsers(groupOutPeer, users)
    case RequestLeaveGroup(groupOutPeer) =>
      handleRequestLeaveGroup(groupOutPeer)
    case RequestRemoveUsers(groupOutPeer, users) =>
      handleRequestRemoveUsers(groupOutPeer, users)
  }

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val id = rand.nextInt

    val group = models.Group(id, currentUser.uid, rand.nextLong(), title)

    withUserOutPeers(users, currentUser) {
      val createGroupModelF = persist.Group.insertEntity(group)

      val userIds = (users map (_.id) toSet) + currentUser.uid

      val addUsersF = userIds map { userId =>
        // TODO: use shapeless-contrib here after upgrading to scala 2.11
        Future.sequence(Seq(
          persist.GroupUser.addUser(group.id, userId),
          persist.UserGroup.addGroup(userId, group.id)
        ))
      }

      // use shapeless, shapeless everywhere!
      val groupCreatedF = for {
        _ <- createGroupModelF
        _ <- Future.sequence(addUsersF)
      } yield {}

      groupCreatedF flatMap { _ =>
        val date = System.currentTimeMillis()

        userIds foreach { userId =>
          for {
            authIds <- getAuthIds(userId)
          } yield {
            authIds foreach { authId =>
              if (authId != currentUser.authId) {
                writeNewUpdate(authId, GroupInvite(
                  groupId = group.id,
                  inviterUserId = currentUser.uid,
                  date = date
                ))
              }
            }
          }
        }

        withNewUpdateState(
          currentUser.authId,
          GroupInvite(
            groupId = group.id,
            inviterUserId = currentUser.uid,
            date = date
          )
        ) { s =>
          val res = ResponseCreateGroup(
            groupPeer = struct.GroupOutPeer(
              id = group.id,
              accessHash = group.accessHash
            ),
            seq = s._1,
            state = Some(s._2),
            users = userIds.toVector
          )
          Ok(res)
        }
      }
    }
  }

  protected def handleRequestInviteUsers(
    groupOutPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupOutPeer.id) map (_.toMap)

    val date = System.currentTimeMillis()

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeers(users, currentUser) {
        userIdsAuthIdsF flatMap { userIdsAuthIds =>
          val userIds = userIdsAuthIds.keySet
          val newUserIds = users map (_.id) toSet

          if (newUserIds.diff(userIds) == newUserIds) {
            val addUsersF = Future.sequence(
              newUserIds map { userId =>
                // TODO: use shapeless-contrib here after upgrading to scala 2.11
                Future.sequence(Seq(
                  persist.GroupUser.addUser(groupId, userId),
                  persist.UserGroup.addGroup(userId, groupId)
                ))
              }
            )

            addUsersF flatMap { _ =>
              val groupInviteUpdate = GroupInvite(
                groupId = groupId,
                inviterUserId = currentUser.uid,
                date = date
              )

              newUserIds foreach { userId =>
                for {
                  authIds <- getAuthIds(userId)
                } yield {
                  authIds foreach (writeNewUpdate(_, groupInviteUpdate))
                }
              }

              val groupUserAddedUpdates = newUserIds map ( userId =>
                GroupUserAdded(
                  groupId = groupId,
                  userId = userId,
                  inviterUserId = currentUser.uid,
                  date = date
                )
              )

              userIdsAuthIds foreach {
                case (userId, authIds) =>
                  authIds foreach { authId =>
                    if (authId != currentUser.authId) {
                      groupUserAddedUpdates foreach (writeNewUpdate(authId, _))
                    }
                  }
              }

              withNewUpdatesState(
                currentUser.authId,
                groupUserAddedUpdates.toSeq
              ) { s =>
                val res = ResponseSeq(s._1, Some(s._2))
                Ok(res)
              }
            }
          } else {
            Future.successful(Error(400, "USER_ALREADY_INVITED", "User is already a member of the group.", false))
          }
        }
      }
    }
  }

  protected def handleRequestLeaveGroup(
    groupOutPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupId)

    val date = System.currentTimeMillis

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      userIdsAuthIdsF flatMap { userIdsAuthIds =>
        if (userIdsAuthIds.toMap.contains(currentUser.uid)) {
          val rmUserF = Future.sequence(Seq(
            persist.GroupUser.removeUser(groupId, currentUser.uid),
            persist.UserGroup.removeGroup(currentUser.uid, groupId)
          ))

          val userLeaveUpdate = GroupUserLeave(
            groupId = groupId,
            userId = currentUser.uid,
            date = date
          )

          userIdsAuthIds foreach {
            case (userId, authIds) =>
              val targetAuthIds = if (userId != currentUser.uid) {
                authIds
              } else {
                authIds.filterNot(_ == currentUser.authId)
              }

              targetAuthIds foreach (writeNewUpdate(_, userLeaveUpdate))
          }

          withNewUpdateState(
            currentUser.authId,
            userLeaveUpdate
          ) { s =>
            val res = ResponseSeq(s._1, Some(s._2))
            Ok(res)
          }
        } else {
          Future.successful(Error(400, "ALREADY_LEFT", "You already left this group.", false))
        }
      }
    }
  }

  protected def handleRequestRemoveUsers(
    groupOutPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val date = System.currentTimeMillis

    withOwnGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeers(users, currentUser) {
        val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupId) map (_.toMap)

        userIdsAuthIdsF flatMap { userIdsAuthIds =>
          val removedUserIds = users map (_.id) toSet

          if (removedUserIds.diff(userIdsAuthIds.keySet) == Set.empty) {
            val removeUsersF = Future.sequence(
              removedUserIds map { userId =>
                // TODO: use shapeless-contrib here after upgrading to scala 2.11
                Future.sequence(Seq(
                  persist.GroupUser.removeUser(groupId, userId),
                  persist.UserGroup.removeGroup(userId, groupId)
                ))
              }
            )

            removeUsersF flatMap { _ =>
              val userKickUpdates = removedUserIds map { userId =>
                GroupUserKick(
                  groupId = groupId,
                  userId = userId,
                  kickerUid = currentUser.uid,
                  date = date
                )
              }

              val targetAuthIds = userIdsAuthIds map {
                case (currentUser.uid, authIds) =>
                  authIds.filterNot(_ == currentUser.authId)
                case (_, authIds) =>
                  authIds
              } flatten

              targetAuthIds foreach { authId =>
                userKickUpdates foreach (writeNewUpdate(authId, _))
              }

              withNewUpdatesState(
                currentUser.authId,
                userKickUpdates.toSeq
              ) { s =>
                val res = ResponseSeq(s._1, Some(s._2))
                Ok(res)
              }
            }
          } else {
            Future.successful(Error(400, "USER_ALREADY_LEFT", "User is not a member of the group.", false))
          }
        }
      }
    }
  }

  protected def handleRequestEditGroupTitle(
    groupOutPeer: struct.GroupOutPeer,
    title: String
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val groupAuthIdsFuture = getGroupUserAuthIds(groupId)

    val date = System.currentTimeMillis

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      persist.Group.updateTitle(groupId, title) flatMap { _ =>
        val titleChangedUpdate =  GroupTitleChanged(
          groupId = groupId,
          userId = currentUser.uid,
          title = title,
          date = date
        )

        for {
          groupAuthIds <- groupAuthIdsFuture
        } yield {
          groupAuthIds foreach { authId =>
            if (authId != currentUser.authId) {
              writeNewUpdate(authId, titleChangedUpdate)
            }
          }
        }

        withNewUpdateState(
          currentUser.authId,
          titleChangedUpdate
        ) { s =>
          val res = ResponseSeq(s._1, Some(s._2))
          Ok(res)
        }
      }
    }
  }

  protected def handleRequestEditGroupAvatar(
    groupPeer: struct.GroupOutPeer,
    fileLocation: models.FileLocation
  ): Future[RpcResponse] = ???

  protected def handleRequestRemoveGroupAvatar(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestDeleteGroup(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???
}
