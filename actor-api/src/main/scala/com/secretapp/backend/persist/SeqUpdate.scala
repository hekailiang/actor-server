package com.secretapp.backend.persist

import com.secretapp.backend.protocol.codecs.message.update._
import com.secretapp.backend.protocol.codecs.message.update.contact._
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.utils.UUIDs
import com.secretapp.backend.data.message.{ update => updateProto }
import com.websudos.phantom.Implicits._
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scodec.Codec
import scodec.bits._

sealed class SeqUpdate extends CassandraTable[SeqUpdate, (Entity[UUID, updateProto.SeqUpdateMessage], Long)] {
  override val tableName = "seq_updates"

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
      case updateProto.UserAvatarChanged.header => decode(row, UserAvatarChangedCodec)
      case updateProto.ChatClear.header => decode(row, ChatClearCodec)
      case updateProto.ChatDelete.header => decode(row, ChatDeleteCodec)
      case updateProto.EncryptedMessage.header => decode(row, EncryptedMessageCodec)
      case updateProto.EncryptedRead.header => decode(row, EncryptedReadCodec)
      case updateProto.EncryptedReadByMe.header => decode(row, EncryptedReadByMeCodec)
      case updateProto.EncryptedReceived.header => decode(row, EncryptedReceivedCodec)
      case updateProto.GroupAvatarChanged.header => decode(row, GroupAvatarChangedCodec)
      case updateProto.GroupInvite.header => decode(row, GroupInviteCodec)
      case updateProto.GroupMembersUpdate.header => decode(row, GroupMembersUpdateCodec)
      case updateProto.GroupTitleChanged.header => decode(row, GroupTitleChangedCodec)
      case updateProto.GroupUserAdded.header => decode(row, GroupUserAddedCodec)
      case updateProto.GroupUserKick.header => decode(row, GroupUserKickCodec)
      case updateProto.GroupUserLeave.header => decode(row, GroupUserLeaveCodec)
      case updateProto.Message.header => decode(row, MessageCodec)
      case updateProto.MessageDelete.header => decode(row, MessageDeleteCodec)
      case updateProto.MessageRead.header => decode(row, MessageReadCodec)
      case updateProto.MessageReadByMe.header => decode(row, MessageReadByMeCodec)
      case updateProto.MessageReceived.header => decode(row, MessageReceivedCodec)
      case updateProto.MessageSent.header => decode(row, MessageSentCodec)
      case updateProto.NameChanged.header => decode(row, NameChangedCodec)
      case updateProto.NewDevice.header => decode(row, NewDeviceCodec)
      case updateProto.RemovedDevice.header => decode(row, RemovedDeviceCodec)
      case updateProto.UpdateConfig.header => decode(row, UpdateConfigCodec)
      case updateProto.PhoneTitleChanged.header => decode(row, PhoneTitleChangedCodec)
      case updateProto.contact.ContactRegistered.header => decode(row, ContactRegisteredCodec)
      case updateProto.contact.ContactsAdded.header => decode(row, ContactsAddedCodec)
      case updateProto.contact.ContactsRemoved.header => decode(row, ContactsRemovedCodec)
      case updateProto.contact.LocalNameChanged.header => decode(row, LocalNameChangedCodec)
    }
  }
}

object SeqUpdate extends SeqUpdate with TableOps {

  def getDifference(authId: Long, state: Option[UUID], limit: Int = 500)(implicit session: Session): Future[immutable.Seq[Entity[UUID, updateProto.SeqUpdateMessage]]] = {
    val query = state match {
      case Some(uuid) =>
        SeqUpdate.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId).and(_.uuid gt uuid)
      case None =>
        SeqUpdate.select.orderBy(_.uuid.asc)
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
    SeqUpdate.select(_.uuid).where(_.authId eqs authId).orderBy(_.uuid.desc).one

  def push(authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] =
    push(UUIDs.timeBased, authId, update)

  def push(uuid: UUID, authId: Long, update: updateProto.SeqUpdateMessage)(implicit session: Session): Future[UUID] = {
    val (body, header) = update match {
      case u: updateProto.UserAvatarChanged => (UserAvatarChangedCodec.encodeValid(u), updateProto.UserAvatarChanged.header)
      case u: updateProto.ChatClear => (ChatClearCodec.encodeValid(u), updateProto.ChatClear.header)
      case u: updateProto.ChatDelete => (ChatDeleteCodec.encodeValid(u), updateProto.ChatDelete.header)
      case u: updateProto.EncryptedMessage => (EncryptedMessageCodec.encodeValid(u), updateProto.EncryptedMessage.header)
      case u: updateProto.EncryptedRead => (EncryptedReadCodec.encodeValid(u), updateProto.EncryptedRead.header)
      case u: updateProto.EncryptedReadByMe => (EncryptedReadByMeCodec.encodeValid(u), updateProto.EncryptedReadByMe.header)
      case u: updateProto.EncryptedReceived => (EncryptedReceivedCodec.encodeValid(u), updateProto.EncryptedReceived.header)
      case u: updateProto.GroupAvatarChanged => (GroupAvatarChangedCodec.encodeValid(u), updateProto.GroupAvatarChanged.header)
      case u: updateProto.GroupInvite => (GroupInviteCodec.encodeValid(u), updateProto.GroupInvite.header)
      case u: updateProto.GroupMembersUpdate => (GroupMembersUpdateCodec.encodeValid(u), updateProto.GroupMembersUpdate.header)
      case u: updateProto.GroupTitleChanged => (GroupTitleChangedCodec.encodeValid(u), updateProto.GroupTitleChanged.header)
      case u: updateProto.GroupUserAdded => (GroupUserAddedCodec.encodeValid(u), updateProto.GroupUserAdded.header)
      case u: updateProto.GroupUserKick => (GroupUserKickCodec.encodeValid(u), updateProto.GroupUserKick.header)
      case u: updateProto.GroupUserLeave => (GroupUserLeaveCodec.encodeValid(u), updateProto.GroupUserLeave.header)
      case u: updateProto.Message => (MessageCodec.encodeValid(u), updateProto.Message.header)
      case u: updateProto.MessageDelete => (MessageDeleteCodec.encodeValid(u), updateProto.MessageDelete.header)
      case u: updateProto.MessageRead => (MessageReadCodec.encodeValid(u), updateProto.MessageRead.header)
      case u: updateProto.MessageReadByMe => (MessageReadByMeCodec.encodeValid(u), updateProto.MessageReadByMe.header)
      case u: updateProto.MessageReceived => (MessageReceivedCodec.encodeValid(u), updateProto.MessageReceived.header)
      case u: updateProto.MessageSent => (MessageSentCodec.encodeValid(u), updateProto.MessageSent.header)
      case u: updateProto.NameChanged => (NameChangedCodec.encodeValid(u), updateProto.NameChanged.header)
      case u: updateProto.NewDevice => (NewDeviceCodec.encodeValid(u), updateProto.NewDevice.header)
      case u: updateProto.RemovedDevice => (RemovedDeviceCodec.encodeValid(u), updateProto.RemovedDevice.header)
      case u: updateProto.UpdateConfig => (UpdateConfigCodec.encodeValid(u), updateProto.UpdateConfig.header)
      case u: updateProto.PhoneTitleChanged => (PhoneTitleChangedCodec.encodeValid(u), updateProto.PhoneTitleChanged.header)
      case u: updateProto.contact.ContactRegistered => (ContactRegisteredCodec.encodeValid(u), updateProto.contact.ContactRegistered.header)
      case u: updateProto.contact.ContactsAdded => (ContactsAddedCodec.encodeValid(u), updateProto.contact.ContactsAdded.header)
      case u: updateProto.contact.ContactsRemoved => (ContactsRemovedCodec.encodeValid(u), updateProto.contact.ContactsRemoved.header)
      case u: updateProto.contact.LocalNameChanged => (LocalNameChangedCodec.encodeValid(u), updateProto.contact.LocalNameChanged.header)
      case _ => throw new Exception("Unknown UpdateMessage")
    }
    val q = insert
      .value(_.authId, authId).value(_.uuid, uuid)
      .value(_.header, header)
      .value(_.protobufBody, body.toByteBuffer)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
    q.map { _ => uuid }
  }
}
