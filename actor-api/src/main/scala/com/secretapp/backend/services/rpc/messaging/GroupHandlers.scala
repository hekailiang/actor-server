package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user.ResponseEditAvatar
import com.secretapp.backend.data.message.struct.Peer
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
import scalaz._
import Scalaz._
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
    case RequestInviteUser(groupOutPeer, user) =>
      handleRequestInviteUser(groupOutPeer, user)
    case RequestLeaveGroup(groupOutPeer) =>
      handleRequestLeaveGroup(groupOutPeer)
    case RequestRemoveUser(groupOutPeer, users) =>
      handleRequestRemoveUser(groupOutPeer, users)
    case RequestEditGroupAvatar(groupOutPeer, fl) =>
      handleRequestEditGroupAvatar(groupOutPeer, fl)
    case RequestRemoveGroupAvatar(groupOutPeer) =>
      handleRequestRemoveGroupAvatar(groupOutPeer)
    case RequestDeleteGroup(groupOutPeer) =>
      handleRequestDeleteGroup(groupOutPeer)
  }

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val id = rand.nextInt(java.lang.Integer.MAX_VALUE)

    val group = models.Group(id, currentUser.uid, rand.nextLong(), title, System.currentTimeMillis)

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

  protected def handleRequestInviteUser(
    groupOutPeer: struct.GroupOutPeer,
    user: struct.UserOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupOutPeer.id) map (_.toMap)

    val date = System.currentTimeMillis()

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeer(user, currentUser) {
        userIdsAuthIdsF flatMap { userIdsAuthIds =>
          val userIds = userIdsAuthIds.keySet

          if (!userIds.contains(user.id)) {
            // TODO: use shapeless-contrib here after upgrading to scala 2.11
            val addUserF = Future.sequence(Seq(
              persist.GroupUser.addUser(groupId, user.id),
              persist.UserGroup.addGroup(user.id, groupId)
            ))

            addUserF flatMap { _ =>
              val groupInviteUpdate = GroupInvite(
                groupId = groupId,
                inviterUserId = currentUser.uid,
                date = date
              )

              for {
                authIds <- getAuthIds(user.id)
              } yield {
                authIds foreach (writeNewUpdate(_, groupInviteUpdate))
              }

              val groupUserAddedUpdate = GroupUserAdded(
                groupId = groupId,
                userId = user.id,
                inviterUserId = currentUser.uid,
                date = date
              )

              userIdsAuthIds foreach {
                case (userId, authIds) =>
                  authIds foreach { authId =>
                    if (authId != currentUser.authId) {
                      writeNewUpdate(authId, groupUserAddedUpdate)
                    }
                  }
              }

              withNewUpdateState(
                currentUser.authId,
                groupUserAddedUpdate
              ) { s =>
                val res = ResponseSeqDate(s._1, Some(s._2), date)
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

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      leaveGroup(groupId, currentUser) map {
        case \/-(state) =>
          Ok(ResponseSeqDate(state._1, Some(state._2), System.currentTimeMillis()))
        case -\/(err) => err
      }
    }
  }

  protected def handleRequestRemoveUser(
    groupOutPeer: struct.GroupOutPeer,
    userOutPeer: struct.UserOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id
    val kickedUserId = userOutPeer.id

    val date = System.currentTimeMillis

    withOwnGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeer(userOutPeer, currentUser) {
        val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupId) map (_.toMap)

        userIdsAuthIdsF flatMap { userIdsAuthIds =>
          if (userIdsAuthIds.keySet.contains(kickedUserId)) {
            // TODO: use shapeless-contrib here after upgrading to scala 2.11
            val removeUserF = Future.sequence(Seq(
              persist.GroupUser.removeUser(groupId, kickedUserId),
              persist.UserGroup.removeGroup(kickedUserId, groupId)
            ))

            removeUserF flatMap { _ =>
              val userKickUpdate = GroupUserKick(
                groupId = groupId,
                userId = kickedUserId,
                kickerUid = currentUser.uid,
                date = date
              )

              val targetAuthIds = userIdsAuthIds map {
                case (currentUser.uid, authIds) =>
                  authIds.filterNot(_ == currentUser.authId)
                case (_, authIds) =>
                  authIds
              } flatten

              targetAuthIds foreach { authId =>
                writeNewUpdate(authId, userKickUpdate)
              }

              withNewUpdateState(
                currentUser.authId,
                userKickUpdate
              ) { s =>
                val res = ResponseSeqDate(s._1, Some(s._2), date)
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
          val res = ResponseSeqDate(s._1, Some(s._2), date)
          Ok(res)
        }
      }
    }
  }

  protected def handleRequestEditGroupAvatar(
    groupOutPeer: struct.GroupOutPeer,
    fileLocation: models.FileLocation
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id
    val date = System.currentTimeMillis()

    withGroupOutPeer(groupOutPeer, currentUser) { group =>
      val sizeLimit: Long = 1024 * 1024 // TODO: configurable

      withValidScaledAvatar(fileRecord, fileLocation) { a =>
        val groupAvatarChangedUpdate = GroupAvatarChanged(
          groupId,
          currentUser.uid,
          a.some,
          date
        )

        persist.Group.updateAvatar(groupId, a) flatMap { _ =>

          foreachGroupUserAuthId(groupId) { authId =>
            writeNewUpdate(authId, groupAvatarChangedUpdate)
          }

          withNewUpdateState(currentUser.authId, groupAvatarChangedUpdate) { s =>
            Ok(ResponseEditGroupAvatar(a, s._1, s._2.some, date))
          }
        }
      }
    }
  }

  protected def handleRequestRemoveGroupAvatar(
    groupOutPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id
    val date = System.currentTimeMillis()

    withGroupOutPeer(groupOutPeer, currentUser) { group =>

      val groupAvatarChangedUpdate = GroupAvatarChanged(
        groupId,
        currentUser.uid,
        None,
        date
      )

      persist.Group.removeAvatar(groupId) flatMap { _ =>

        foreachGroupUserAuthId(groupId) { authId =>
          writeNewUpdate(authId, groupAvatarChangedUpdate)
        }

        withNewUpdateState(currentUser.authId, groupAvatarChangedUpdate) { s =>
          Ok(ResponseSeqDate(s._1, s._2.some, date))
        }
      }
    }
  }

  protected def handleRequestDeleteGroup(
    groupOutPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id
    val date = System.currentTimeMillis

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>

      getGroupUserIdsWithAuthIds(groupId).map(_.toMap) flatMap { userIdsAuthIds =>

        val userLeaveUpdate = GroupUserLeave(
          groupId = groupId,
          userId = currentUser.uid,
          date = date
        )

        val chatDeleteUpdate = ChatDelete(Peer.group(groupId))

        val removeUserF = Future.sequence(
          Seq(
            persist.GroupUser.removeUser(groupId, currentUser.uid),
            persist.UserGroup.removeGroup(currentUser.uid, groupId)
          )
        )

        removeUserF flatMap { _ =>
          val allExceptCurrentAuthIds = userIdsAuthIds.filterKeys(_ != currentUser.uid).map(_._2).flatten

          allExceptCurrentAuthIds foreach { authId =>
            writeNewUpdate(authId, userLeaveUpdate)
          }

          val currentAuthIds = userIdsAuthIds.filterKeys(_ == currentUser.uid).head._2

          currentAuthIds foreach { authId =>
            writeNewUpdate(authId, chatDeleteUpdate)
          }

          withNewUpdateState(
            currentUser.authId,
            chatDeleteUpdate
          ) { s =>
            val res = ResponseSeqDate(s._1, Some(s._2), date)
            Ok(res)
          }
        }
      }
    }
  }
}
