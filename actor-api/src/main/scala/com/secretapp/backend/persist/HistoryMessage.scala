package com.secretapp.backend.persist

import com.google.protobuf.ByteString
import com.secretapp.backend.data.message.rpc.history
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._
import scodec.bits._

object HistoryMessage extends SQLSyntaxSupport[history.HistoryMessage] {
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
    "is_read"
  )

  lazy val hm = HistoryMessage.syntax("hm")
  private val isNotDeleted = sqls.eq(hm.column("is_deleted"), false)

  def apply(hm: SyntaxProvider[history.HistoryMessage])(rs: WrappedResultSet): history.HistoryMessage = apply(hm.resultName)(rs)

  def apply(hm: ResultName[history.HistoryMessage])(rs: WrappedResultSet): history.HistoryMessage = history.HistoryMessage(
    senderUserId = rs.int(hm.senderUserId),
    randomId = rs.long(hm.randomId),
    date = rs.get[DateTime](hm.date).getMillis,
    message = MessageContent.fromProto(
      protobuf.MessageContent(
        `type` = rs.int(hm.column("message_content_header")),
        content = ByteString.copyFrom({
          val bs = rs.binaryStream(hm.column("message_content_data"))
          // TODO: don't use BitVector
          val bv = BitVector.fromInputStream(bs)
          bs.close()
          bv.toByteArray
        })
      )
    )
  )

  def create(
    userId: Int,
    peer: struct.Peer,
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent
  )(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[history.HistoryMessage] = Future {
    blocking {
      withSQL {
        insert.into(HistoryMessage).namedValues(
          column.column("user_id") -> userId,
          column.column("peer_type") -> peer.typ.intType,
          column.column("peer_id") -> peer.id,
          column.column("date") -> date,
          column.randomId -> randomId,
          column.senderUserId -> senderUserId,
          column.column("message_content_header") -> message.header,
          column.column("message_content_data") -> message.toProto.content.toByteArray
        )
      }.execute.apply

      history.HistoryMessage(senderUserId, randomId, date.getMillis, message)
    }
  }

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[List[history.HistoryMessage]] =
    Future {
      withSQL {
        select.from(HistoryMessage as hm)
          .where.append(isNotDeleted)
          .and.append(sqls"${where}")
      }.map(HistoryMessage(hm)).list.apply()
    }

  def findAll(userId: Int, peer: struct.Peer, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = HistoryMessage.autoSession
  ): Future[List[history.HistoryMessage]] =
    findAllBy(
      sqls.eq(hm.column("user_id"), userId)
        .and.eq(hm.column("peer_type"), peer.typ.intType)
        .and.eq(hm.column("peer_id"), peer.id)
        .and.ge(hm.column("date"), startDate)
        .orderBy(hm.column("date")).desc
        .limit(limit)
    )

  def countUnread(userId: Int, peer: struct.Peer)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Long] = Future {
    blocking {
      withSQL {
        select(sqls.count(sqls"1")).from(HistoryMessage as hm)
          .where.append(isNotDeleted)
          .and.eq(hm.column("user_id"), userId)
          .and.eq(hm.column("peer_type"), peer.typ.intType)
          .and.eq(hm.column("peer_id"), peer.id)
          .and.eq(hm.column("is_read"), false)
      }.map(_.long(1)).single.apply getOrElse (0)
    }
  }

  def countBefore(userId: Int, peer: struct.Peer, date: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Long] = Future {
    blocking {
      withSQL {
        select(sqls.count).from(HistoryMessage as hm)
          .where.append(isNotDeleted)
          .and.eq(hm.column("user_id"), userId)
          .and.eq(hm.column("peer_type"), peer.typ.intType)
          .and.eq(hm.column("peer_id"), peer.id)
          .and.le(hm.date, date)
      }.map(_.long(1)).single.apply() getOrElse (0)
    }
  }

  def markAllAsRead(userId: Int, peer: struct.Peer, beforeDate: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.column("is_read") -> true
        )
          .where.eq(column.column("is_deleted"), false)
          .and.eq(column.column("user_id"), userId)
          .and.eq(column.column("peer_type"), peer.typ.intType)
          .and.eq(column.column("peer_id"), peer.id)
          .and.eq(column.column("is_read"), false)
      }.update.apply
    }
  }

  def destroy(userId: Int, peer: struct.Peer, randomId: Long)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.column("is_deleted") -> true
        )
          .where.eq(column.column("user_id"), userId)
          .and.eq(column.column("peer_type"), peer.typ.intType)
          .and.eq(column.column("peer_id"), peer.id)
          .and.eq(column.randomId, randomId)
      }.update.apply
    }
  }

  def destroyAll(userId: Int, peer: struct.Peer)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(HistoryMessage).set(
          column.column("is_deleted") -> true
        )
          .where.eq(column.column("user_id"), userId)
          .and.eq(column.column("peer_type"), peer.typ.intType)
          .and.eq(column.column("peer_id"), peer.id)
      }.update.apply
    }
  }
}
