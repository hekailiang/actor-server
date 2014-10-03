package com.secretapp.backend.persist

import com.secretapp.backend.protocol.codecs.message.update._
import java.nio.ByteBuffer
import com.datastax.driver.core.ConsistencyLevel
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.struct.{User, AvatarImage, Avatar}
import com.websudos.phantom.query.ExecutableStatement
import com.secretapp.backend.data.message.update.SeqUpdate
import com.secretapp.backend.data.message.{ update => updateProto }
import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.protocol.codecs.message.update.SeqUpdateMessageCodec
import com.gilt.timeuuid._
import java.util.UUID
import scala.collection.immutable
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scodec.bits._
import scodec.codecs.{ uuid => uuidCodec }
import scalaz._
import Scalaz._
import im.actor.messenger.api.{User => ProtoUser}

sealed class SeqUpdateRecord extends CassandraTable[SeqUpdateRecord, Entity[UUID, updateProto.SeqUpdateMessage]] {
  override lazy val tableName = "seq_updates"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object uuid extends TimeUUIDColumn(this) with PrimaryKey[UUID]

  object header extends IntColumn(this) {
    override lazy val name = "header"
  }

  object protobufBody extends BlobColumn(this) {
    override lazy val name = "protobuf_column"
  }

  override def fromRow(row: Row): Entity[UUID, updateProto.SeqUpdateMessage] = {
    header(row) match {
      case 1L =>
        Entity(uuid(row),
          MessageCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case 2L =>
        Entity(uuid(row),
          NewDeviceCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case 3L =>
        Entity(uuid(row),
          NewYourDeviceCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case 4L =>
        Entity(uuid(row),
          MessageSentCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.AvatarChanged.seqUpdateHeader =>
        Entity(uuid(row),
          AvatarChangedCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.ContactRegistered.seqUpdateHeader =>
        Entity(uuid(row),
          ContactRegisteredCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.MessageReceived.seqUpdateHeader =>
        Entity(uuid(row),
          MessageReceivedCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.MessageRead.seqUpdateHeader =>
        Entity(uuid(row),
          MessageReadCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.GroupInvite.seqUpdateHeader =>
        Entity(uuid(row),
          GroupInviteCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.GroupMessage.seqUpdateHeader =>
        Entity(uuid(row),
          GroupMessageCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.GroupUserAdded.seqUpdateHeader =>
        Entity(uuid(row),
          GroupUserAddedCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
      case updateProto.GroupUserLeave.seqUpdateHeader =>
        Entity(uuid(row),
          GroupUserLeaveCodec.decode(BitVector(protobufBody(row))).toOption.get._2)
    }

  }
}

object SeqUpdateRecord extends SeqUpdateRecord with DBConnector {

  // TODO: limit by size, not rows count
  def getDifference(authId: Long, state: Option[UUID], limit: Int = 500)(implicit session: Session): Future[immutable.Seq[Entity[UUID, updateProto.SeqUpdateMessage]]] = {
    //select.where(c => QueryCondition(QueryBuilder.gte(c.uuid.name, QueryBuilder.fcall("maxTimeuuid")))).limit(limit)
    //  .fetch
    val query = state match {
      case Some(uuid) =>
        SeqUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId).and(_.uuid gt uuid)
      case None =>
        SeqUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId)
    }

    query.limit(limit).fetch map (_.toList)
  }

  def getState(authId: Long)(implicit session: Session): Future[Option[UUID]] =
    SeqUpdateRecord.select(_.uuid).where(_.authId eqs authId).orderBy(_.uuid.desc).one

  def push(authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] = {
    val uuid = TimeUuid()
    push(uuid, authId, update)
  }

  def push(uuid: UUID, authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] = {
    val q = update match {
      // TODO: DRY
      case u: updateProto.Message =>
        val body = MessageCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.Message.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageSent =>
        val body = MessageSentCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageSent.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.NewDevice =>
        val body = NewDeviceCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.NewDevice.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.NewYourDevice =>
        val body = NewYourDeviceCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.NewYourDevice.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.AvatarChanged =>
        val body = AvatarChangedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.AvatarChanged.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.ContactRegistered =>
        val body = ContactRegisteredCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.ContactRegistered.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageReceived =>
        val body = MessageReceivedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageReceived.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageRead =>
        val body = MessageReadCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageRead.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupInvite =>
        val body = GroupInviteCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupInvite.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupMessage =>
        val body = GroupMessageCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupMessage.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupUserAdded =>
        val body = GroupUserAddedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupUserAdded.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupUserLeave =>
        val body = GroupUserLeaveCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupUserLeave.seqUpdateHeader).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case _ =>
        throw new Exception("Unknown UpdateMessage")
    }

    val f = q.consistencyLevel_=(ConsistencyLevel.ALL).future

    f map (_ => uuid)
  }

}
