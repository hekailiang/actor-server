package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.persist.GroupUserRecord
import scala.concurrent.Future

trait GroupHelpers extends UserHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.dispatcher

  def withGroupUserAuthIds(groupId: Int)(f: Seq[Long] => Any) = {
    GroupUserRecord.getUsers(groupId) map {
      case groupUserIds =>
        groupUserIds foreach { groupUserId =>
          for {
            authIds <- getAuthIds(groupUserId)
          } yield {
            f(authIds)
          }
        }
    }
  }
}
