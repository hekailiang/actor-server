package com.secretapp.backend.persist

import com.secretapp.backend.models
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._
import scodec.bits._

object HistoryMessage extends SQLSyntaxSupport[models.HistoryMessage] {
  override val tableName = "history_messages"
  override val columnNames = Seq(
    "user_id",
    "peer_type",
    "peer_id",
    "date",
    "random_id",
    "sender_user_id",
    "message_content_header",
    "message_content_data",
    "is_deleted",
    "state"
  )

  lazy val hm = HistoryMessage.syntax("hm")
  private val isNotDeleted = sqls.eq(hm.column("is_deleted"), false)

  def apply(hm: SyntaxProvider[models.HistoryMessage])(rs: WrappedResultSet): models.HistoryMessage = apply(hm.resultName)(rs)

  def apply(hm: ResultName[models.HistoryMessage])(rs: WrappedResultSet): models.HistoryMessage = models.HistoryMessage(
    userId = rs.int(hm.userId),
    peer = models.Peer(
      models.PeerType.fromInt(rs.int(hm.column("peer_type"))),
      rs.int(hm.column("peer_id"))
    ),
    randomId = rs.long(hm.randomId),
    date = rs.get[DateTime](hm.date),
    senderUserId = rs.int(hm.senderUserId),
    messageContentHeader = rs.int(hm.messageContentHeader),
    messageContentData = {
      val bs = rs.binaryStream(hm.messageContentData)
      val bv = BitVector.fromInputStream(bs)
      bs.close()
      bv
    },
    state = models.MessageState.fromInt(rs.int(hm.state))
  )

  def create(
    userId: Int,
    peer: models.Peer,
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    messageContentHeader: Int,
    messageContentBytes: BitVector,
    state: models.MessageState
  )(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[models.HistoryMessage] = Future {
    blocking {
      withSQL {
        insert.into(HistoryMessage).namedValues(
          column.userId -> userId,
          column.column("peer_type") -> peer.typ.toInt,
          column.column("peer_id") -> peer.id,
          column.date -> date,
          column.randomId -> randomId,
          column.senderUserId -> senderUserId,
          column.messageContentHeader -> messageContentHeader,
          column.messageContentData -> messageContentBytes.toByteArray,
          column.state -> state.toInt
        )
      }.execute.apply

      models.HistoryMessage(
        userId,
        peer,
        date,
        randomId,
        senderUserId,
        messageContentHeader,
        messageContentBytes,
        state
      )
    }
  }

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[List[models.HistoryMessage]] =
    Future {
      withSQL {
        select.from(HistoryMessage as hm)
          .where.append(isNotDeleted)
          .and.append(sqls"${where}")
      }.map(HistoryMessage(hm)).list.apply()
    }

  def findAll(userId: Int, peer: models.Peer, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[List[models.HistoryMessage]] =
    findAllBy(
      sqls.eq(hm.userId, userId)
        .and.eq(hm.column("peer_type"), peer.typ.toInt)
        .and.eq(hm.column("peer_id"), peer.id)
        .and.ge(hm.date, startDate)
        .orderBy(hm.date).desc
        .limit(limit)
    )

  def countUnread(userId: Int, peer: models.Peer)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[Long] = Future {
    blocking {
      withSQL {
        select(sqls.count(sqls"1")).from(HistoryMessage as hm)
          .where.append(isNotDeleted)
          .and.eq(hm.userId, userId)
          .and.eq(hm.column("peer_type"), peer.typ.toInt)
          .and.eq(hm.column("peer_id"), peer.id)
          .and.not.eq(hm.senderUserId, userId)
          .and.not.eq(hm.state, models.MessageState.Read.toInt)
      }.map(_.long(1)).single.apply getOrElse (0)
    }
  }

  def updateStateOfSentBefore(userId: Int, peer: models.Peer, beforeDate: DateTime, state: models.MessageState)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.state -> state.toInt
        )
          .where.eq(column.column("is_deleted"), false)
          .and.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
          .and.eq(column.senderUserId, userId)
          .and.not.eq(column.state, state.toInt)
          .and.le(column.date, beforeDate)
      }.update.apply
    }
  }

  def updateStateOfReceivedBefore(userId: Int, peer: models.Peer, beforeDate: DateTime, state: models.MessageState)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.state -> state.toInt
        )
          .where.eq(column.column("is_deleted"), false)
          .and.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
          .and.not.eq(column.senderUserId, userId)
          .and.not.eq(column.state, state.toInt)
          .and.le(column.date, beforeDate)
      }.update.apply
    }
  }

  def destroy(userId: Int, peer: models.Peer, randomId: Long)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.column("is_deleted") -> true
        )
          .where.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
          .and.eq(column.randomId, randomId)
      }.update.apply
    }
  }

  def destroyAll(userId: Int, peer: models.Peer)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.column("is_deleted") -> true
        )
          .where.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
      }.update.apply
    }
  }

  case class UsersCount(receivedMsgCount: Int, sentMsgCount: Int, lastMessageAt: DateTime)

  def getUsersCounts(userIds: Seq[Int])
                    (implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession): Future[Seq[(Int, UsersCount)]] = {
    if (userIds.isEmpty) Future.successful(Seq.empty)
    else Future {
      blocking {
        val q = sql"""
              select user_id,
              sum(sent_count) as sent_count,
              sum(received_count) as received_count,
              max(last_message_at) as last_message_at
              from (
                select user_id, 0 as sent_count, count(*) as received_count, max(date) as last_message_at
                from $table
                where state = ${models.MessageState.Received.toInt} and user_id IN ($userIds)
                group by user_id

                union all

                select user_id, count(*) as sent_count, 0 as received_count, max(date) as last_message_at
                from $table
                where state = ${models.MessageState.Sent.toInt} and user_id IN ($userIds)
                group by user_id
              ) as users_counts
              group by user_id
              """
        q.map { rs =>
          val uc = UsersCount(
            receivedMsgCount = rs.int("received_count"),
            sentMsgCount = rs.int("sent_count"),
            lastMessageAt = rs.get[DateTime]("last_message_at")
          )
          (rs.int("user_id"), uc)
        }.list().apply()
      }
    }
  }
}
