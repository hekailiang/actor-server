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

sealed class DialogUnreadCounter extends CassandraTable[DialogUnreadCounter, Long] {
  override val tableName = "dialog_unread_counters"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object peerType extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_type"
  }
  object peerId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_id"
  }
  object unreadCount extends CounterColumn(this)

  def fromRow(row: Row): Long = {
    unreadCount(row)
  }
}

object DialogUnreadCounter extends DialogUnreadCounter with TableOps {
  def increment(userId: Int, peer: struct.Peer)(implicit session: Session): Future[ResultSet] = {
    update
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .modify(_.unreadCount increment 1)
      .future()
  }

  def decrement(userId: Int, peer: struct.Peer, num: Long)(implicit session: Session): Future[ResultSet] = {
    update
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .modify(_.unreadCount decrement num)
      .future()
  }

  def getCount(userId: Int, peer: struct.Peer)(implicit session: Session): Future[Long] = {
    select
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .one() map (_.getOrElse(0))
  }

  def deleteByUserAndPeer(userId: Int, peer: struct.Peer)(implicit session: Session): Future[ResultSet] = {
    delete
      .where(_.userId eqs userId)
      .and(_.peerType eqs peer.typ.intType)
      .and(_.peerId eqs peer.id)
      .future()
  }
}
