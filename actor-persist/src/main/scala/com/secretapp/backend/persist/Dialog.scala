package com.secretapp.backend.persist

import com.secretapp.backend.models
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._
import scodec.bits._

object Dialog extends SQLSyntaxSupport[models.Dialog] {
  override val tableName = "dialogs"
  override val columnNames = Seq(
    "user_id",
    "peer_type",
    "peer_id",
    "sort_date",
    "sender_user_id",
    "random_id",
    "date",
    "message_content_header",
    "message_content_data",
    "state"
  )

  lazy val d = Dialog.syntax("d")

  def apply(d: SyntaxProvider[models.Dialog])(rs: WrappedResultSet): models.Dialog = apply(d.resultName)(rs)

  def apply(d: ResultName[models.Dialog])(rs: WrappedResultSet): models.Dialog = models.Dialog(
    userId = rs.int(d.userId),
    peer = models.Peer(
      typ = models.PeerType.fromInt(rs.int(d.column("peer_type"))),
      id = rs.int(d.column("peer_id"))
    ),
    senderUserId = rs.int(d.senderUserId),
    sortDate = rs.get[DateTime](d.sortDate),
    randomId = rs.long(d.randomId),
    date = rs.get[DateTime](d.date),
    messageContentHeader = rs.int(d.messageContentHeader),
    messageContentData = {
      val bs = rs.binaryStream(d.messageContentData)
      // TODO: don't use BitVector
      val bv = BitVector.fromInputStream(bs)
      bs.close()
      bv
    },
    state = models.MessageState.fromInt(rs.int(d.state))
  )

  def updateMessage(
    userId: Int,
    peer: models.Peer,
    messageContentHeader: Int,
    messageContentData: BitVector
  )(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(Dialog).set(
          column.messageContentHeader -> messageContentHeader,
          column.messageContentData -> messageContentData.toByteArray
        )
          .where.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
      }.update.apply
    }
  }

  def existsSync(
    userId: Int,
    peer: models.Peer
  )(
    implicit session: DBSession
  ): Boolean = {
    sql"""
    select exists(
      select 1 from dialogs where user_id = ${userId} and peer_type = ${peer.typ.toInt} and peer_id = ${peer.id} limit 1
    ) as e
    """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)
  }

  def createOrUpdate(
    userId: Int,
    peer: models.Peer,
    sortDateOpt: Option[DateTime],
    senderUserId: Int,
    randomIdOpt: Option[Long],
    dateOpt: Option[DateTime],
    messageOpt: Option[(Int, BitVector)],
    state: models.MessageState
  )(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[Unit] = Future {
    blocking {
      existsSync(userId, peer)(session) match {
        case true =>
          withSQL {
            // It is a hack, we cannot just make Seq and pass it like vals: _* because of scalikejdbc #253 issue
            val sortDateSql = sortDateOpt match {
              case Some(sortDate) => sortDate
              case None => sqls"sort_date"
            }

            val randomIdSql = randomIdOpt match {
              case Some(randomId) => randomId
              case None => sqls"random_id"
            }

            val dateSql = dateOpt match {
              case Some(date) => date
              case None => sqls"date"
            }

            val (
              messageContentHeaderSql,
              messageContentDataSql
            ) = messageOpt match {
              case Some((header, data)) => (header, data.toByteArray)
              case None => (
                sqls"message_content_header",
                sqls"message_content_data"
              )
            }

            update(Dialog).set(
              column.senderUserId -> senderUserId,
              column.randomId -> randomIdSql,
              column.date -> dateSql,
              column.sortDate -> sortDateSql,
              column.messageContentHeader -> messageContentHeaderSql,
              column.messageContentData -> messageContentDataSql,
              column.state -> state.toInt
            )
              .where.eq(column.userId, userId)
              .and.eq(column.column("peer_type"), peer.typ.toInt)
              .and.eq(column.column("peer_id"), peer.id)
          }.update.apply
        case false =>
          val sortDate = sortDateOpt.getOrElse(new DateTime(0))
          val randomId = randomIdOpt.getOrElse(0)
          val date = dateOpt.getOrElse(new DateTime(0))

          val (
            messageContentHeader,
            messageContentData
          ) = messageOpt match {
            case Some((header, data)) => (header, data.toByteArray)
            case None => (0x01, Array[Byte]()) // same as TextMessage("")
          }

          withSQL {
            insert.into(Dialog).namedValues(
              column.userId -> userId,
              column.column("peer_type") -> peer.typ.toInt,
              column.column("peer_id") -> peer.id,
              column.senderUserId -> senderUserId,
              column.sortDate -> sortDate,
              column.randomId -> randomId,
              column.date -> date,
              column.messageContentHeader -> messageContentHeader,
              column.messageContentData -> messageContentData,
              column.state -> state.toInt
            )
          }.execute.apply
      }

      ()
    }
  }

  def updateStateIfFresh(userId: Int, peer: models.Peer, senderUserId: Int, date: DateTime, state: models.MessageState)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[Int] =
    Future {
      blocking {
        withSQL {
          update(Dialog).set(
            column.state -> state.toInt
          )
            .where.eq(column.userId, userId)
            .and.eq(column.column("peer_type"), peer.typ.toInt)
            .and.eq(column.column("peer_id"), peer.id)
            .and.eq(column.senderUserId, senderUserId)
            .and.le(column.date, date)
        }.update.apply
      }
    }

  def findAll(userId: Int, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[List[models.Dialog]] = Future {
    blocking {
       withSQL {
        select.from(Dialog as d)
          .where.eq(d.userId, userId)
          .and.ge(d.date, startDate)
          .orderBy(d.sortDate).desc
          .limit(limit)
       }.map(Dialog(d)).list.apply
    }
  }

  def findAllWithUnreadCount(userId: Int, startDate: DateTime, limit: Int)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[List[(models.Dialog, Long)]] =
    findAll(userId, startDate, limit) flatMap { ds =>
      val futures =
        for (d <- ds)
        yield HistoryMessage.countUnread(userId, d.peer) map ((d, _))

      Future.sequence(futures)
    }

  def destroy(userId: Int, peer: models.Peer)(
    implicit ec: ExecutionContext, session: DBSession = Dialog.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(Dialog)
          .where.eq(column.userId, userId)
          .and.eq(column.column("peer_type"), peer.typ.toInt)
          .and.eq(column.column("peer_id"), peer.id)
      }.execute.apply
    }
  }
}
