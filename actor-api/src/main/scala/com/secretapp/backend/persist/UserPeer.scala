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

// TODO: use model here instead of history.Dialog

sealed class UserPeer extends CassandraTable[UserPeer, struct.Peer] {
  override val tableName = "user_peers"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object peerType extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_type"
  }
  object peerId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "peer_id"
  }

  override def fromRow(row: Row): struct.Peer = {
    struct.Peer(
      typ = struct.PeerType.fromInt(peerType(row)),
      id = peerId(row)
    )
  }
}

object UserPeer extends UserPeer with TableOps {
  def insertEntity(
    userId: Int,
    peer: struct.Peer
  )(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.userId, userId)
      .value(_.peerType, peer.typ.intType)
      .value(_.peerId, peer.id)
      .future()
  }

  def getUserPeers(userId: Int)(implicit session: Session): Future[Seq[struct.Peer]] = {
    for {
      peerDatas <- select(_.peerType, _.peerId).where(_.userId eqs userId).fetch()
    } yield {
      peerDatas map {
        case (peerType, peerId) =>
          struct.Peer(
            typ = struct.PeerType.fromInt(peerType),
            id = peerId
          )
      }
    }
  }
}
