package com.secretapp.backend.persist

import com.secretapp.backend.protocol.codecs.message.update._
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.utils.UUIDs
import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.struct.{User, AvatarImage, Avatar}
import com.websudos.phantom.query.ExecutableStatement
import com.secretapp.backend.data.message.update.SeqUpdate
import com.secretapp.backend.data.message.{ update => updateProto }
import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.protocol.codecs.message.update.SeqUpdateMessageCodec
import java.nio.ByteBuffer
import java.util.UUID
import scala.collection.immutable
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scodec.Codec
import scodec.bits._
import scodec.codecs.{ uuid => uuidCodec }
import scalaz._
import Scalaz._
import im.actor.messenger.api.{User => ProtoUser}

sealed class SeqUpdateRecord extends CassandraTable[SeqUpdateRecord, (Entity[UUID, updateProto.SeqUpdateMessage], Long)] {
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

  override def fromRow(row: Row): (Entity[UUID, updateProto.SeqUpdateMessage], Long) = {
    @inline
    def decode[A](row: Row, codec: Codec[A]): (Entity[UUID, updateProto.SeqUpdateMessage], Long) = {
      val protoBody = BitVector(protobufBody(row))
      (
        Entity(
          uuid(row),
          codec.decode(protoBody).toOption.get._2.asInstanceOf[updateProto.SeqUpdateMessage]
        ),
        protoBody.length / 8
      )
    }

    header(row) match {
      case updateProto.Message.header =>
        decode(row, MessageCodec)
      case updateProto.NewDevice.header =>
        decode(row, NewDeviceCodec)
      case updateProto.NewFullDevice.header =>
        decode(row, NewFullDeviceCodec)
      case updateProto.RemoveDevice.header =>
        decode(row, RemoveDeviceCodec)
      case updateProto.MessageSent.header =>
        decode(row, MessageSentCodec)
      case updateProto.AvatarChanged.header =>
        decode(row, AvatarChangedCodec)
      case updateProto.NameChanged.header =>
        decode(row, NameChangedCodec)
      case updateProto.ContactRegistered.header =>
        decode(row, ContactRegisteredCodec)
      case updateProto.MessageReceived.header =>
        decode(row, MessageReceivedCodec)
      case updateProto.MessageRead.header =>
        decode(row, MessageReadCodec)
      case updateProto.GroupInvite.header =>
        decode(row, GroupInviteCodec)
      case updateProto.GroupMessage.header =>
        decode(row, GroupMessageCodec)
      case updateProto.GroupUserAdded.header =>
        decode(row, GroupUserAddedCodec)
      case updateProto.GroupUserLeave.header =>
        decode(row, GroupUserLeaveCodec)
      case updateProto.GroupUserKick.header =>
        decode(row, GroupUserKickCodec)
      case updateProto.GroupCreated.header =>
        decode(row, GroupCreatedCodec)
      case updateProto.GroupTitleChanged.header =>
        decode(row, GroupTitleChangedCodec)
      case updateProto.GroupAvatarChanged.header =>
        decode(row, GroupAvatarChangedCodec)
      case updateProto.contact.UpdateContactRegistered.header =>
        decode(row, contact.UpdateContactRegisteredCodec)
      case updateProto.contact.UpdateContactsAdded.header =>
        decode(row, contact.UpdateContactsAddedCodec)
      case updateProto.contact.UpdateContactsRemoved.header =>
        decode(row, contact.UpdateContactsRemovedCodec)
    }

  }
}

object SeqUpdateRecord extends SeqUpdateRecord with DBConnector {
  /*
  def processMessages(processorId: String, partitionNr: Long, optMarker: Option[String] = None)(f: PersistenceMessage => Any)(implicit session: Session) = {
    def process(sequenceNr: Long): Unit = {
      val baseQuery = select
        .where(_.processorId eqs processorId).and(_.partitionNr eqs partitionNr)
        .and(_.sequenceNr eqs sequenceNr)
      val query = optMarker match {
        case Some(marker) =>
          baseQuery.and(_.marker eqs marker)
        case None => baseQuery
      }
      println(query.queryString)
      query.one() map {
        case Some(message) =>
          f(message)
          process(sequenceNr + 1)
        case None =>
          println(s"stopped at $sequenceNr")
      }
    }

    process(1)
  }*/

  def getDifference(authId: Long, state: Option[UUID], limit: Int = 500)(implicit session: Session): Future[immutable.Seq[Entity[UUID, updateProto.SeqUpdateMessage]]] = {
    val query = state match {
      case Some(uuid) =>
        SeqUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId).and(_.uuid gt uuid)
      case None =>
        SeqUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId)
    }

    // TODO: use iteratee
    for {
      sizedUpdates <- query.limit(limit).fetch
    } yield {
      @annotation.tailrec
      def collect(
        sizedUpdates: Seq[(Entity[UUID, updateProto.SeqUpdateMessage], Long)],
        updates: immutable.Seq[Entity[UUID, updateProto.SeqUpdateMessage]],
        size: Long
      ): immutable.Seq[Entity[UUID, updateProto.SeqUpdateMessage]] = {
        if (sizedUpdates == Nil) {
          updates
        } else {
          val (update, updateSize) = sizedUpdates.head
          // LIMIT 50 Kb
          if (size + updateSize > 50 * 1024) {
            updates
          } else {
            collect(sizedUpdates.tail, updates :+ update, size + updateSize)
          }
        }
      }

      collect(sizedUpdates, Nil, 0)
    }
  }

  def getState(authId: Long)(implicit session: Session): Future[Option[UUID]] =
    SeqUpdateRecord.select(_.uuid).where(_.authId eqs authId).orderBy(_.uuid.desc).one

  def push(authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] = {
    val uuid = UUIDs.timeBased
    push(uuid, authId, update)
  }

  def push(uuid: UUID, authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] = {
    val q = update match {
      // TODO: DRY
      case u: updateProto.Message =>
        val body = MessageCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.Message.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageSent =>
        val body = MessageSentCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageSent.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.NewDevice =>
        val body = NewDeviceCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.NewDevice.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.NewFullDevice =>
        val body = NewFullDeviceCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.NewFullDevice.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.RemoveDevice =>
        val body = RemoveDeviceCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.RemoveDevice.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.AvatarChanged =>
        val body = AvatarChangedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.AvatarChanged.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.NameChanged =>
        val body = NameChangedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.NameChanged.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.ContactRegistered =>
        val body = ContactRegisteredCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.ContactRegistered.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageReceived =>
        val body = MessageReceivedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageReceived.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.MessageRead =>
        val body = MessageReadCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.MessageRead.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupInvite =>
        val body = GroupInviteCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupInvite.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupMessage =>
        val body = GroupMessageCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupMessage.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupUserAdded =>
        val body = GroupUserAddedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupUserAdded.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupUserLeave =>
        val body = GroupUserLeaveCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupUserLeave.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupUserKick =>
        val body = GroupUserKickCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupUserKick.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupCreated =>
        val body = GroupCreatedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupCreated.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupTitleChanged =>
        val body = GroupTitleChangedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupTitleChanged.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.GroupAvatarChanged =>
        val body = GroupAvatarChangedCodec.encode(u)
        insert.value(_.authId, authId).value(_.uuid, uuid).value(_.header, updateProto.GroupAvatarChanged.header).value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.contact.UpdateContactRegistered =>
        val body = contact.UpdateContactRegisteredCodec.encode(u)
        insert
          .value(_.authId, authId).value(_.uuid, uuid)
          .value(_.header, updateProto.contact.UpdateContactRegistered.header)
          .value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.contact.UpdateContactsAdded =>
        val body = contact.UpdateContactsAddedCodec.encode(u)
        insert
          .value(_.authId, authId).value(_.uuid, uuid)
          .value(_.header, updateProto.contact.UpdateContactsAdded.header)
          .value(_.protobufBody, body.toOption.get.toByteBuffer)
      case u: updateProto.contact.UpdateContactsRemoved =>
        val body = contact.UpdateContactsRemovedCodec.encode(u)
        insert
          .value(_.authId, authId).value(_.uuid, uuid)
          .value(_.header, updateProto.contact.UpdateContactsRemoved.header)
          .value(_.protobufBody, body.toOption.get.toByteBuffer)
      case _ =>
        throw new Exception("Unknown UpdateMessage")
    }

    val f = q.consistencyLevel_=(ConsistencyLevel.ALL).future

    f map (_ => uuid)
  }

}
