package com.secretapp.backend.persist

import com.datastax.driver.core.utils.UUIDs
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.history
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import scala.concurrent.Future
import com.google.protobuf.ByteString

// TODO: use model here instead of history.HistoryMessage

sealed class HistoryMessage extends CassandraTable[HistoryMessage, history.HistoryMessage] {
  override val tableName = "history_messages"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object peerType extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "peer_type"
  }
  object peerId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "peer_id"
  }
  object date extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "date"
  }
  object randomId extends LongColumn(this) with Index[Long] {
    override lazy val name = "random_id"
  }
  object senderUserId extends IntColumn(this) {
    override lazy val name = "sender_user_id"
  }
  object messageContentHeader extends IntColumn(this) {
    override lazy val name = "message_content_header"
  }
  object messageContentBytes extends BlobColumn(this) {
    override lazy val name = "message_content_bytes"
  }
  object isDeleted extends BooleanColumn(this) with Index[Boolean] {
    override lazy val name = "is_deleted"
  }

  override def fromRow(row: Row): history.HistoryMessage = {
    history.HistoryMessage(
      senderUserId = senderUserId(row),
      randomId = randomId(row),
      date = date(row),
      message = MessageContent.fromProto(
        protobuf.MessageContent(
          `type` = messageContentHeader(row),
          content = ByteString.copyFrom(messageContentBytes(row))
        )
      )
    )
  }
}

object HistoryMessage extends HistoryMessage with TableOps {
  def insertEntity(
    userId: Int,
    peer: struct.Peer,
    date: Long,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent
  )(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.userId, userId)
      .value(_.peerType, peer.typ.intType)
      .value(_.peerId, peer.id)
      .value(_.date, date)
      .value(_.randomId, randomId)
      .value(_.senderUserId, senderUserId)
      .value(_.messageContentHeader, message.header)
      .value(_.messageContentBytes, message.toProto.content.asReadOnlyByteBuffer)
      .value(_.isDeleted, false)
      .future()
  }

  def deleteByPeer(userId: Int, peer: struct.Peer)(implicit session: Session): Future[ResultSet] = {
    delete
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .future()
  }

  def fetchByPeer(userId: Int, peer: struct.Peer, startDate: Long, limit: Int)(implicit session: Session): Future[Seq[history.HistoryMessage]] = {
    select
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .and(_.date gte startDate)
      .and(_.isDeleted eqs false)
      .limit(limit)
      .fetch()
  }

  def fetchOneBefore(userId: Int, peer: struct.Peer, beforeDate: Long)(implicit session: Session): Future[Option[history.HistoryMessage]] = {
    select
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .and(_.date lte beforeDate)
      .one()
  }

  @deprecated("count is not accurate in cassandra", "")
  def fetchCountBefore(userId: Int, peer: struct.Peer, beforeDate: Long)(implicit session: Session): Future[Long] = {
    count
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .and(_.date lte beforeDate)
      .and(_.isDeleted eqs false)
      .one() map (_.getOrElse(0))
  }

  // rethink this
  def setDeleted(userId: Int, peer: struct.Peer, randomId: Long)(implicit session: Session): Future[Unit] = {
    select(_.date)
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .and(_.randomId eqs randomId)
      .one() flatMap {
      case Some(date) =>
        insert
          .value(_.userId, userId)
          .value(_.peerType, peer.typ.intType)
          .value(_.peerId, peer.id)
          .value(_.date, date)
          .value(_.randomId, randomId)
          .value(_.isDeleted, false)
          .future() map (_ => ())
      case None =>
        Future.successful(())
    }
  }
}
