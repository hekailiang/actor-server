package com.secretapp.backend.helpers

import akka.actor._
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.{ RpcResponse, Error}
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.models.FileLocation
import com.secretapp.backend.persist
import com.secretapp.backend.util.AvatarUtils
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import scalaz.Scalaz._

trait GroupHelpers extends UserHelpers with UpdatesHelpers {
  val context: ActorContext
  implicit val session: CSession
  implicit val timeout: Timeout

  import context.dispatcher

  def getGroupStruct(groupId: Int, currentUserId: Int)(implicit s: ActorSystem): Future[Option[struct.Group]] = {
    for {
      optGroupModelWithAvatar <- persist.Group.findWithAvatar(groupId)
      groupUserMembers <- persist.GroupUser.getUserIdsWithMeta(groupId)
    } yield {
      optGroupModelWithAvatar map {
        case (group, avatarData) =>
          struct.Group.fromModel(
            group = group,
            groupMembers = groupUserMembers.toVector map {
              case (userId, persist.GroupUserMeta(inviterUserId, date)) =>
                struct.Member(userId, inviterUserId, date)
            },
            isMember = groupUserMembers.find(_._1 == currentUserId).isDefined,
            optAvatar = avatarData.avatar
          )
      }
    }
  }

  def getGroupUserIds(groupId: Int): Future[Seq[Int]] = {
    persist.GroupUser.getUserIds(groupId)
  }

  def getGroupUserIdsWithAuthIds(groupId: Int): Future[Seq[(Int, Seq[Long])]] = {
    persist.GroupUser.getUserIds(groupId) flatMap { userIds =>
      Future.sequence(
        userIds map { userId =>
          getAuthIds(userId) map ((userId, _))
        }
      )
    }
  }

  def getGroupMembersWithAuthIds(groupId: Int): Future[Seq[(struct.Member, Seq[Long])]] = {
    persist.GroupUser.getUserIdsWithMeta(groupId) flatMap { userIdsWithMeta =>
      Future.sequence(
        userIdsWithMeta map {
          case (userId, persist.GroupUserMeta(inviterUserId, date)) =>
            getAuthIds(userId) map ((struct.Member(userId, inviterUserId, date), _))
        }
      )
    }
  }

  def getGroupUserAuthIds(groupId: Int): Future[Seq[Long]] = {
    getGroupUserIds(groupId) flatMap {
      case groupUserIds =>
        Future.sequence(
          groupUserIds map { groupUserId =>
            getAuthIds(groupUserId)
          }
        ) map (_.flatten)
    }
  }

  def foreachGroupUserAuthId(groupId: Int)(f: Long => Any) =
    getGroupUserAuthIds(groupId) map {
      _ foreach f
    }

  def withValidAvatar(fr: persist.File, fl: FileLocation)(f: => Future[RpcResponse]): Future[RpcResponse] =
    fr.getFileLength(fl.fileId.toInt) flatMap { len =>
      val sizeLimit: Long = 1024 * 1024 // TODO: configurable

      if (len > sizeLimit)
        Future successful Error(400, "FILE_TOO_BIG", "", false)
      else
        f
    }

  def withScaledAvatar(fr: persist.File, fl: FileLocation)
                      (f: models.Avatar => Future[RpcResponse])
                      (implicit ec: ExecutionContext, timeout: Timeout, s: ActorSystem): Future[RpcResponse] =
    AvatarUtils.scaleAvatar(fr, fl) flatMap f recover {
      case e =>
        log.warning(s"Failed while updating avatar: $e")
        Error(400, "IMAGE_LOAD_ERROR", "", false)
    }

  def withValidScaledAvatar(fr: persist.File, fl: FileLocation)
                           (f: models.Avatar => Future[RpcResponse])
                           (implicit ec: ExecutionContext, timeout: Timeout, s: ActorSystem): Future[RpcResponse] =
    withValidAvatar(fr, fl) {
      withScaledAvatar(fr, fl)(f)
    }

  def leaveGroup(groupId: Int, randomId: Long, currentUser: models.User): Future[Error \/ UpdatesBroker.StrictState] = {
    val userIdsAuthIdsF = getGroupUserIdsWithAuthIds(groupId)
    val date = System.currentTimeMillis()

    userIdsAuthIdsF flatMap { userIdsAuthIds =>
      if (userIdsAuthIds.toMap.contains(currentUser.uid)) {
        val rmUserF = Future.sequence(Seq(
          persist.GroupUser.removeUser(groupId, currentUser.uid),
          persist.UserGroup.removeGroup(currentUser.uid, groupId)
        ))

        val userLeaveUpdate = GroupUserLeave(
          groupId = groupId,
          randomId = randomId,
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

        writeNewUpdateAndGetState(
          currentUser.authId,
          userLeaveUpdate
        ) map (_.right)
      } else {
        Future.successful(Error(400, "ALREADY_LEFT", "You already left this group.", false).left)
      }
    }
  }
}
