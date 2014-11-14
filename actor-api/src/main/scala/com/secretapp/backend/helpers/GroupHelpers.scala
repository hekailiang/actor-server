package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.Future

trait GroupHelpers extends UserHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.dispatcher

  def getGroupStruct(groupId: Int, currentUserId: Int)(implicit s: ActorSystem): Future[Option[struct.Group]] = {
    for {
      optGroupModelWithAvatar <- persist.Group.getEntityWithAvatar(groupId)
      groupUserIds <- persist.GroupUser.getUserIds(groupId)
    } yield {
      optGroupModelWithAvatar map {
        case (group, avatarData) =>
          struct.Group.fromModel(
            group = group,
            groupUserIds = groupUserIds.toVector,
            isMember = groupUserIds.contains(currentUserId),
            optAvatar = avatarData.avatar
          )
      }
    }
  }

  def getGroupUserAuthIds(groupId: Int): Future[Seq[Long]] = {
    persist.GroupUser.getUserIds(groupId) flatMap {
      case groupUserIds =>
        Future.sequence(
          groupUserIds map { groupUserId =>
            getAuthIds(groupUserId)
          }
        ) map (_.flatten)
    }
  }

  def withGroupUserAuthIds(groupId: Int)(f: Seq[Long] => Any) = {
    getGroupUserAuthIds(groupId) map f
  }
}
