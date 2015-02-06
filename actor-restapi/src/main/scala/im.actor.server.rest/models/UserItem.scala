package im.actor.server.rest.models

import com.secretapp.backend.persist._
import org.joda.time.DateTime
import scala.concurrent.{ ExecutionContext, Future }

case class UserItem(id: Int, name: String, phoneNumber: Option[Long], contactsCount: Int, receivedMsgCount: Int,
                    sentMsgCount: Int, lastSeenAt: Option[DateTime], lastMessageAt: Option[DateTime])

object UserItem {
  def paginate(req: Map[String, String] = Map())
              (implicit ec: ExecutionContext): Future[(Seq[UserItem], Int)] =
    for {
      (users, totalCount) <- User.all(req)
      userIds = users.map(_._1)
      phoneNumbers <- UserPhone.getLatestNumbers(userIds)
      contactsCount <- contact.UserContact.getCountactsCount(userIds)
      msgCounters <- HistoryMessage.getUsersCounts(userIds)
    } yield {
      val phoneNumbersMap = phoneNumbers.toMap
      val contactsCountMap = contactsCount.toMap
      val msgCountersMap = msgCounters.toMap
      val entries = users.map { u =>
        val msgCounter = msgCountersMap.get(u._1)
        UserItem(
          id = u._1,
          name = u._2,
          phoneNumber = phoneNumbersMap.get(u._1),
          contactsCount = contactsCountMap.getOrElse(u._1, 0),
          receivedMsgCount = msgCounter.map(_.receivedMsgCount).getOrElse(0),
          sentMsgCount = msgCounter.map(_.sentMsgCount).getOrElse(0),
          lastSeenAt = None, // TODO
          lastMessageAt = msgCounter.map(_.lastMessageAt)
        )
      }
      (entries, totalCount)
    }

  def show(userId: Int)
          (implicit ec: ExecutionContext): Future[Option[UserItem]] =
    User.find(userId)(None).flatMap {
      case Some(user) =>
        for {
          phoneNumbers <- UserPhone.findFirstByUserId(user.uid)
          contactsCount <- contact.UserContact.getCountactsCount(Seq(user.uid))
          msgCounters <- HistoryMessage.getUsersCounts(Seq(user.uid))
        } yield {
          val msgCountersItem = msgCounters.headOption.map(_._2)
          val contactsCountItem = contactsCount.headOption.map(_._2)
          Some(UserItem(
            id = user.uid,
            name = user.name,
            phoneNumber = phoneNumbers.map(_.number),
            contactsCount = contactsCountItem.getOrElse(0),
            receivedMsgCount = msgCountersItem.map(_.receivedMsgCount).getOrElse(0),
            sentMsgCount = msgCountersItem.map(_.sentMsgCount).getOrElse(0),
            lastSeenAt = None, // TODO
            lastMessageAt = msgCountersItem.map(_.lastMessageAt)
          ))
        }
      case None => Future.successful(None)
    }
}
