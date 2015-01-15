package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.eaio.uuid.UUID
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
import com.secretapp.backend.util.{ ACL, AvatarUtils }
import org.joda.time.DateTime
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
    case RequestEditGroupTitle(groupPeer, randomId, title) =>
      handleRequestEditGroupTitle(groupPeer, randomId, title)
    case RequestInviteUser(groupOutPeer, randomId, user) =>
      handleRequestInviteUser(groupOutPeer, randomId, user)
    case RequestLeaveGroup(groupOutPeer, randomId) =>
      handleRequestLeaveGroup(groupOutPeer, randomId)
    case RequestKickUser(groupOutPeer, randomId, users) =>
      handleRequestKickUser(groupOutPeer, randomId, users)
    case RequestEditGroupAvatar(groupOutPeer, randomId, fl) =>
      handleRequestEditGroupAvatar(groupOutPeer, randomId, fl)
    case RequestRemoveGroupAvatar(groupOutPeer, randomId) =>
      handleRequestRemoveGroupAvatar(groupOutPeer, randomId)
  }

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val id = rand.nextInt(java.lang.Integer.MAX_VALUE)

    val dateTime = new DateTime
    val date = dateTime.getMillis

    val group = models.Group(id, currentUser.uid, rand.nextLong(), title, dateTime)

    withUserOutPeers(users, currentUser) {
      val createGroupModelF = persist.Group.create(
        id = group.id,
        creatorUserId = group.creatorUserId,
        accessHash = group.accessHash,
        title = group.title,
        createdAt = group.createdAt,
        randomId = randomId
      )

      val userIds = (users map (_.id) toSet) + currentUser.uid

      val addUsersF = userIds map (
        persist.GroupUser.addGroupUser(group.id, _, currentUser.uid, dateTime)
      )

      // use shapeless, shapeless everywhere!
      val groupCreatedF = for {
        _ <- createGroupModelF
        _ <- Future.sequence(addUsersF)
      } yield {}

      groupCreatedF flatMap { _ =>
        userIds foreach { userId =>
          for {
            authIds <- getAuthIds(userId)
          } yield {
            authIds foreach { authId =>
              if (authId != currentUser.authId) {
                writeNewUpdate(authId, GroupInvite(
                  groupId = group.id,
                  randomId = randomId,
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
            randomId = randomId,
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
    randomId: Long,
    user: struct.UserOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val groupWithMetaFuture = persist.Group.findWithAvatarAndChangeMeta(groupId)

    val membersAuthIdsF = getGroupMembersWithAuthIds(groupOutPeer.id)

    val dateTime = new DateTime
    val date = dateTime.getMillis

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeer(user, currentUser) {
        membersAuthIdsF flatMap { membersAuthIds =>
          val userIds = membersAuthIds.map(_._1.id).toSet

          if (!userIds.contains(user.id)) {
            val addUserF = persist.GroupUser.addGroupUser(groupId, user.id, currentUser.uid, dateTime)

            // FIXME: add user AFTER we got Some(groupWithMeta)
            addUserF flatMap { _ =>
              groupWithMetaFuture onFailure {
                case e =>
                  println(s"eeee $e")
              }
              for {
                groupWithMetaOpt <- groupWithMetaFuture
              } yield {
                groupWithMetaOpt match {
                  case Some((group, avatarData, titleChangeMeta, avatarChangeMeta)) =>
                    val targetUserUpdates = Vector(
                      GroupInvite(
                        groupId = groupId,
                        randomId = randomId,
                        inviterUserId = currentUser.uid,
                        date = date
                      ),
                      GroupTitleChanged(
                        groupId = groupId,
                        randomId = titleChangeMeta.randomId,
                        userId = titleChangeMeta.userId,
                        group.title,
                        date = titleChangeMeta.date.getMillis
                      ),
                      GroupAvatarChanged(
                        groupId = groupId,
                        randomId = avatarChangeMeta.randomId,
                        userId = avatarChangeMeta.userId,
                        avatar = avatarData.avatar,
                        date = titleChangeMeta.date.getMillis
                      ),
                      GroupMembersUpdate(
                        groupId = groupId,
                        members = (membersAuthIds map (_._1) toVector) :+ struct.Member(user.id, currentUser.uid, date)
                      )
                    )

                    broadcastUserUpdates(user.id, targetUserUpdates)
                  case None =>
                    throw new Exception("Cannot get group with meta")
                }

              }

              val groupUserAddedUpdate = GroupUserAdded(
                groupId = groupId,
                randomId = randomId,
                userId = user.id,
                inviterUserId = currentUser.uid,
                date = date
              )

              membersAuthIds foreach {
                case (struct.Member(userId, _, _), authIds) =>
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
    groupOutPeer: struct.GroupOutPeer,
    randomId: Long
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      leaveGroup(groupId, randomId, currentUser) map {
        case \/-(state) =>
          Ok(ResponseSeqDate(state._1, Some(state._2), System.currentTimeMillis()))
        case -\/(err) => err
      }
    }
  }

  protected def handleRequestKickUser(
    groupOutPeer: struct.GroupOutPeer,
    randomId: Long,
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
            val removeUserF = persist.GroupUser.removeGroupUser(groupId, kickedUserId)

            removeUserF flatMap { _ =>
              val userKickUpdate = GroupUserKick(
                groupId = groupId,
                randomId = randomId,
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
    randomId: Long,
    title: String
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val groupAuthIdsFuture = getGroupUserAuthIds(groupId)

    val dateTime = new DateTime
    val date = dateTime.getMillis

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      persist.Group.updateTitle(groupId, title, currentUser.uid, randomId, dateTime) flatMap { _ =>
        val titleChangedUpdate =  GroupTitleChanged(
          groupId = groupId,
          randomId = randomId,
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
    randomId: Long,
    fileLocation: models.FileLocation
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val dateTime = new DateTime
    val date = dateTime.getMillis

    withGroupOutPeer(groupOutPeer, currentUser) { group =>
      val sizeLimit: Long = 1024 * 1024 // TODO: configurable

      withValidScaledAvatar(fileRecord, fileLocation) { a =>
        val groupAvatarChangedUpdate = GroupAvatarChanged(
          groupId,
          randomId,
          currentUser.uid,
          a.some,
          date
        )

        persist.Group.updateAvatar(groupId, a, currentUser.uid, randomId, dateTime) flatMap { _ =>

          foreachGroupUserAuthId(groupId) { authId =>
            if (authId != currentUser.authId)
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
    groupOutPeer: struct.GroupOutPeer,
    randomId: Long
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val dateTime = new DateTime
    val date = dateTime.getMillis

    withGroupOutPeer(groupOutPeer, currentUser) { group =>

      val groupAvatarChangedUpdate = GroupAvatarChanged(
        groupId = groupId,
        randomId = randomId,
        userId = currentUser.uid,
        avatar = None,
        date = date
      )

      persist.Group.removeAvatar(groupId, currentUser.uid, randomId, dateTime) flatMap { _ =>

        foreachGroupUserAuthId(groupId) { authId =>
          if (authId != currentUser.authId)
            writeNewUpdate(authId, groupAvatarChangedUpdate)
        }

        withNewUpdateState(currentUser.authId, groupAvatarChangedUpdate) { s =>
          Ok(ResponseSeqDate(s._1, s._2.some, date))
        }
      }
    }
  }
}
