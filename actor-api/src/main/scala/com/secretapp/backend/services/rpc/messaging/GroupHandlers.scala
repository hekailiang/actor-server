package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
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
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz._
import Scalaz._
import scodec.bits._

trait GroupHandlers extends RandomService
    with UserHelpers
    with GroupHelpers
    with GroupAvatarHelpers
    with PeerHelpers
    with UpdatesHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

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

  object ServiceMessages {
    def groupCreated = ServiceMessage("Group created", Some(GroupCreatedExtension()))
    def userAdded(userId: Int) = ServiceMessage("User added to the group", Some(UserAddedExtension(userId)))
    def userLeft(userId: Int) = ServiceMessage("User left the group", Some(UserLeftExtension()))
    def userKicked(userId: Int) = ServiceMessage("User kicked from the group", Some(UserKickedExtension(userId)))
    def changedTitle(title: String) = ServiceMessage("Group title changed", Some(GroupChangedTitleExtension(title)))
    def changedAvatar(avatar: Option[models.Avatar]) = ServiceMessage(
      "Group avatar changed",
      Some(GroupChangedAvatarExtension(avatar))
    )
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
        group <- createGroupModelF
        _ <- Future.sequence(addUsersF)
      } yield group

      groupCreatedF flatMap { group =>
        val serviceMessage = ServiceMessages.groupCreated

        userIds foreach { userId =>
          writeHistoryMessage(
            userId,
            models.Peer.group(group.id),
            dateTime,
            randomId,
            currentUser.uid,
            serviceMessage,
            models.MessageState.Sent
          )

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
    userOutPeer: struct.UserOutPeer
  ): Future[RpcResponse] = {
    val groupId = groupOutPeer.id

    val dateTime = new DateTime
    val date = dateTime.getMillis

    val groupWithMetaFuture = persist.Group.findWithAvatarAndChangeMeta(groupId)
    val membersAuthIdsFuture = getGroupMembersWithAuthIds(groupOutPeer.id)

    withGroupOutPeer(groupOutPeer, currentUser) { _ =>
      withUserOutPeer(userOutPeer, currentUser) {
        membersAuthIdsFuture flatMap { membersUserIdsAuthIds =>
          val userIds = membersUserIdsAuthIds.map(_._1.id).toSet

          if (!userIds.contains(userOutPeer.id)) {
            val addUserF = persist.GroupUser.addGroupUser(groupId, userOutPeer.id, currentUser.uid, dateTime)

            // FIXME: add user AFTER we got Some(groupWithMeta)
            addUserF flatMap { _ =>
              groupWithMetaFuture onFailure {
                case e =>
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
                        members = (membersUserIdsAuthIds map (_._1) toVector) :+ struct.Member(userOutPeer.id, currentUser.uid, date)
                      )
                    )

                    broadcastUserUpdates(userOutPeer.id, targetUserUpdates)
                  case None =>
                    throw new Exception("Cannot get group with meta")
                }
              }

              val serviceMessage = ServiceMessages.userAdded(userOutPeer.id)

              (userIds + userOutPeer.id) foreach (userId => writeHistoryMessage(
                userId,
                groupOutPeer.asPeer.asModel,
                dateTime,
                randomId,
                currentUser.uid,
                serviceMessage,
                models.MessageState.Sent
              ))

              val groupUserAddedUpdate = GroupUserAdded(
                groupId = groupId,
                randomId = randomId,
                userId = userOutPeer.id,
                inviterUserId = currentUser.uid,
                date = date
              )

              membersUserIdsAuthIds foreach {
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

    for (userIds <- getGroupUserIds(groupId)) {
      val dateTime = new DateTime

      val serviceMessage = ServiceMessages.userLeft(currentUser.uid)

      userIds foreach (userId => writeHistoryMessage(
        userId,
        groupOutPeer.asPeer.asModel,
        dateTime,
        randomId,
        currentUser.uid,
        serviceMessage,
        models.MessageState.Sent,
        false
      ))
    }

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

    val dateTime = new DateTime
    val date = dateTime.getMillis

    withKickableGroupMember(groupOutPeer, currentUser, userOutPeer) { _ =>
      withUserOutPeer(userOutPeer, currentUser) {
        val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupId) map (_.toMap)

        userIdsAuthIdsF flatMap { userIdsAuthIds =>
          if (userIdsAuthIds.keySet.contains(kickedUserId)) {
            val userIds = userIdsAuthIds.map(_._1).toSet

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

              val serviceMessage = ServiceMessages.userKicked(userOutPeer.id)

              userIds foreach (writeHistoryMessage(
                _,
                groupOutPeer.asPeer.asModel,
                dateTime,
                randomId,
                currentUser.uid,
                serviceMessage,
                models.MessageState.Sent
              ))

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

    val groupUserIdsAuthIdsFuture = getGroupUserIdsWithAuthIds(groupId)

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
          groupUserIdsAuthIds <- groupUserIdsAuthIdsFuture
        } yield {
          val serviceMessage = ServiceMessages.changedTitle(title)

          groupUserIdsAuthIds foreach {
            case (userId, authIds) =>
              writeHistoryMessage(
                userId,
                groupOutPeer.asPeer.asModel,
                dateTime,
                randomId,
                currentUser.uid,
                serviceMessage,
                models.MessageState.Sent
              )

              authIds foreach { authId =>
                if (authId != currentUser.authId) {
                  writeNewUpdate(authId, titleChangedUpdate)
                }
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

      withValidScaledAvatar(fileLocation) { a =>
        val groupAvatarChangedUpdate = GroupAvatarChanged(
          groupId,
          randomId,
          currentUser.uid,
          a.some,
          date
        )

        persist.Group.updateAvatar(groupId, a, currentUser.uid, randomId, dateTime) flatMap { _ =>
          val serviceMessage = ServiceMessages.changedAvatar(Some(a))

          foreachGroupUserIdsWithAuthIds(groupId) {
            case (userId, authIds) =>
              writeHistoryMessage(
                userId,
                models.Peer.group(group.id),
                dateTime,
                randomId,
                currentUser.uid,
                serviceMessage,
                models.MessageState.Sent
              )

              authIds foreach { authId =>
                if (authId != currentUser.authId)
                  writeNewUpdate(authId, groupAvatarChangedUpdate)
              }
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
        val serviceMessage = ServiceMessages.changedAvatar(None)

        foreachGroupUserIdsWithAuthIds(groupId) {
          case (userId, authIds) =>
            writeHistoryMessage(
              userId,
              models.Peer.group(group.id),
              dateTime,
              randomId,
              currentUser.uid,
              serviceMessage,
              models.MessageState.Sent
            )

            authIds foreach { authId =>
              if (authId != currentUser.authId)
                writeNewUpdate(authId, groupAvatarChangedUpdate)
            }
        }

        withNewUpdateState(currentUser.authId, groupAvatarChangedUpdate) { s =>
          Ok(ResponseSeqDate(s._1, s._2.some, date))
        }
      }
    }
  }
}
