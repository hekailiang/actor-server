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

case class DialogMeta(peer: struct.Peer, senderUserId: Int, randomId: Long, date: Long, message: MessageContent)

sealed class Dialog extends CassandraTable[Dialog, DialogMeta] {
  override val tableName = "dialogs"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object peerType extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_type"
  }
  object peerId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_id"
  }
  object senderUserId extends IntColumn(this) {
    override lazy val name = "sender_user_id"
  }
  object randomId extends LongColumn(this) {
    override lazy val name = "random_id"
  }
  object date extends LongColumn(this)
  object messageContentHeader extends IntColumn(this) {
    override lazy val name = "message_content_header"
  }
  object messageContentBytes extends BlobColumn(this) {
    override lazy val name = "message_content_bytes"
  }

  def fromRow(row: Row): DialogMeta = {
    DialogMeta(
      struct.Peer(
        struct.PeerType.fromInt(peerType(row)),
        peerId(row)
      ),
      senderUserId(row),
      randomId(row),
      date(row),
      MessageContent.fromProto(
        protobuf.MessageContent(
          `type` = messageContentHeader(row),
          content = ByteString.copyFrom(messageContentBytes(row))
        )
      )
    )
  }
}

object Dialog extends Dialog with TableOps {
  def updateEntity(
    userId: Int,
    peer: struct.Peer,
    senderUserId: Int,
    randomId: Long,
    date: Long,
    message: MessageContent
  )(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.userId, userId)
      .value(_.peerType, peer.typ.intType)
      .value(_.peerId, peer.id)
      .value(_.senderUserId, senderUserId)
      .value(_.randomId, randomId)
      .value(_.date, date)
      .value(_.messageContentHeader, message.header)
      .value(_.messageContentBytes, message.toProto.content.asReadOnlyByteBuffer)
      .future()
  }

  def deleteByUserAndPeer(userId: Int, peer: struct.Peer)(implicit session: Session): Future[ResultSet] = {
    delete
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .future()
  }

  // rethink this
  def fetchDialogs(
    userId: Int,
    startDate: Long,
    limit: Int
  )(implicit session: Session): Future[Seq[DialogMeta]] = {
    select
      .where(_.userId eqs userId)
      .fetch() map { dialogs =>
      dialogs.sortBy(- _.date).take(limit)
    }
  }
}
