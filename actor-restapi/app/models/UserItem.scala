package models

import com.secretapp.backend.persist._
import org.joda.time.DateTime
import scala.concurrent.{ ExecutionContext, Future }

case class UserItem(id: Int, name: String, phone: Long, contactsCount: Int, receivedMsgCount: Int,
                    sentMsgCount: Int, lastSeenAt: Option[DateTime], lastMessageAt: Option[DateTime])

object UserItem {
  def paginate(req: Map[String, Seq[String]] = Map())
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
        phone = phoneNumbersMap.getOrElse(u._1, 0L),
        contactsCount = contactsCountMap.getOrElse(u._1, 0),
        receivedMsgCount = msgCounter.map(_.receivedMsgCount).getOrElse(0),
        sentMsgCount = msgCounter.map(_.sentMsgCount).getOrElse(0),
        lastSeenAt = None,
        lastMessageAt = msgCounter.map(_.lastMessageAt)
      )
    }
    (entries, totalCount)
  }
}
