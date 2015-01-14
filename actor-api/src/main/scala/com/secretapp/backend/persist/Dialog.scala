package com.secretapp.backend.persist

import com.google.protobuf.ByteString
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.struct
import im.actor.messenger.{ api => protobuf }
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._
import scodec.bits._

case class DialogMeta(peer: struct.Peer, senderUserId: Int, randomId: Long, date: DateTime, message: MessageContent)

object Dialog extends SQLSyntaxSupport[DialogMeta] {
  override val tableName = "dialogs"
  override val columnNames = Seq(
    "user_id",
    "peer_type",
    "peer_id",
    "sender_user_id",
    "random_id",
    "date",
    "message_content_header",
    "message_content_data"
  )

  lazy val d = Dialog.syntax("d")

  def apply(d: SyntaxProvider[DialogMeta])(rs: WrappedResultSet): DialogMeta = apply(d.resultName)(rs)

  def apply(d: ResultName[DialogMeta])(rs: WrappedResultSet): DialogMeta = DialogMeta(
    peer = struct.Peer(
      typ = struct.PeerType.fromInt(rs.int(d.column("peer_type"))),
      id = rs.int(d.column("peer_id"))
    ),
    senderUserId = rs.int(d.senderUserId),
    randomId = rs.long(d.randomId),
    date = rs.get[DateTime](d.date),
    message = MessageContent.fromProto(
      protobuf.MessageContent(
        `type` = rs.int(d.column("message_content_header")),
        content = ByteString.copyFrom({
          val bs = rs.binaryStream(d.column("message_content_data"))
          // TODO: don't use BitVector
          val bv = BitVector.fromInputStream(bs)
          bs.close()
          bv.toByteArray
        })
      )
    )
  )

  def existsSync(
    userId: Int,
    peer: struct.Peer
  )(
    implicit session: DBSession
  ): Boolean = {
    sql"""
    select exists(
      select 1 from dialogs where user_id = ${userId} and peer_type = ${peer.typ.intType} and peer_id = ${peer.id} limit 1
    ) as e
    """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)
  }

  def createOrUpdate(
    userId: Int,
    peer: struct.Peer,
    senderUserId: Int,
    randomId: Long,
    date: DateTime,
    message: MessageContent
  )(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[DialogMeta] = Future {
    blocking {
      existsSync(userId, peer)(session) match {
        case true =>
          withSQL {
            update(Dialog).set(
              column.senderUserId -> senderUserId,
              column.randomId -> randomId,
              column.date -> date,
              column.column("message_content_header") -> message.header,
              column.column("message_content_data") -> message.toProto.content.toByteArray
            )
          }.update.apply
        case false =>
          withSQL {
            insert.into(Dialog).namedValues(
              column.column("user_id") -> userId,
              column.column("peer_type") -> peer.typ.intType,
              column.column("peer_id") -> peer.id,
              column.senderUserId -> senderUserId,
              column.randomId -> randomId,
              column.date -> date,
              column.column("message_content_header") -> message.header,
              column.column("message_content_data") -> message.toProto.content.toByteArray
            )
          }.execute.apply
      }
    }

    DialogMeta(peer, senderUserId, randomId, date, message)
  }

  def findAll(userId: Int, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[List[DialogMeta]] = Future {
    blocking {
       withSQL {
        select.from(Dialog as d)
          .where.eq(d.column("user_id"), userId)
          .and.ge(d.column("date"), startDate)
          .orderBy(d.column("date")).desc
          .limit(limit)
       }.map(Dialog(d)).list.apply
    }
  }

  def findAllWithUnreadCount(userId: Int, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[List[(DialogMeta, Long)]] =
    findAll(userId, startDate, limit) flatMap { dms =>
      val futures =
        for (dm <- dms)
        yield HistoryMessage.countUnread(userId, dm.peer) map ((dm, _))

      Future.sequence(futures)
    }

  def destroy(userId: Int, peer: struct.Peer)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(Dialog)
          .where.eq(column.column("user_id"), userId)
          .and.eq(column.column("peer_type"), peer.typ.intType)
          .and.eq(column.column("peer_id"), peer.id)
      }.execute.apply
    }
  }
}
