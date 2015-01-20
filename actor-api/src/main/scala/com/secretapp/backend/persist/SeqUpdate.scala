package com.secretapp.backend.persist

import com.eaio.uuid.UUID
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.protocol.codecs.message.update._
import com.secretapp.backend.protocol.codecs.message.update.contact._
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.immutable
import scalikejdbc._
import scodec.Codec
import scodec.bits._

object SeqUpdate extends SQLSyntaxSupport[(Entity[UUID, updateProto.SeqUpdateMessage], Long)] {
  type UpdateWithSize = (Entity[UUID, updateProto.SeqUpdateMessage], Long)

  override val tableName = "seq_updates"
  override val columnNames = Seq("auth_id", "uuid", "header", "protobuf_data")

  lazy val upd = SeqUpdate.syntax("upd")

  def apply(upd: SyntaxProvider[UpdateWithSize])(rs: WrappedResultSet): UpdateWithSize = apply(upd.resultName)(rs)

  def apply(upd: ResultName[UpdateWithSize])(rs: WrappedResultSet): UpdateWithSize = {
    @inline
    def decode[A](rs: WrappedResultSet, codec: Codec[A]): (Entity[UUID, updateProto.SeqUpdateMessage], Long) = {
      val protobufData = {
        val bs = rs.binaryStream(upd.column("protobuf_data"))
        val bv = BitVector.fromInputStream(bs)
        bs.close()
        bv
      }

      val uuid = new UUID(rs.string(upd.column("uuid")))
      val length = protobufData.length / 8

      (
        Entity(
          uuid,
          codec.decode(protobufData).toOption.get._2.asInstanceOf[updateProto.SeqUpdateMessage]
        ),
        length
      )
    }

    val header = rs.int(upd.column("header"))

    header match {
      case updateProto.UserAvatarChanged.header => decode(rs, UserAvatarChangedCodec)
      case updateProto.ChatClear.header => decode(rs, ChatClearCodec)
      case updateProto.ChatDelete.header => decode(rs, ChatDeleteCodec)
      case updateProto.EncryptedMessage.header => decode(rs, EncryptedMessageCodec)
      case updateProto.EncryptedRead.header => decode(rs, EncryptedReadCodec)
      case updateProto.EncryptedReadByMe.header => decode(rs, EncryptedReadByMeCodec)
      case updateProto.EncryptedReceived.header => decode(rs, EncryptedReceivedCodec)
      case updateProto.GroupAvatarChanged.header => decode(rs, GroupAvatarChangedCodec)
      case updateProto.GroupInvite.header => decode(rs, GroupInviteCodec)
      case updateProto.GroupMembersUpdate.header => decode(rs, GroupMembersUpdateCodec)
      case updateProto.GroupTitleChanged.header => decode(rs, GroupTitleChangedCodec)
      case updateProto.GroupUserAdded.header => decode(rs, GroupUserAddedCodec)
      case updateProto.GroupUserKick.header => decode(rs, GroupUserKickCodec)
      case updateProto.GroupUserLeave.header => decode(rs, GroupUserLeaveCodec)
      case updateProto.Message.header => decode(rs, MessageCodec)
      case updateProto.MessageDelete.header => decode(rs, MessageDeleteCodec)
      case updateProto.MessageRead.header => decode(rs, MessageReadCodec)
      case updateProto.MessageReadByMe.header => decode(rs, MessageReadByMeCodec)
      case updateProto.MessageReceived.header => decode(rs, MessageReceivedCodec)
      case updateProto.MessageSent.header => decode(rs, MessageSentCodec)
      case updateProto.NameChanged.header => decode(rs, NameChangedCodec)
      case updateProto.NewDevice.header => decode(rs, NewDeviceCodec)
      case updateProto.RemovedDevice.header => decode(rs, RemovedDeviceCodec)
      case updateProto.UpdateConfig.header => decode(rs, UpdateConfigCodec)
      case updateProto.PhoneTitleChanged.header => decode(rs, PhoneTitleChangedCodec)
      case updateProto.contact.ContactRegistered.header => decode(rs, ContactRegisteredCodec)
      case updateProto.contact.ContactsAdded.header => decode(rs, ContactsAddedCodec)
      case updateProto.contact.ContactsRemoved.header => decode(rs, ContactsRemovedCodec)
      case updateProto.contact.LocalNameChanged.header => decode(rs, LocalNameChangedCodec)
    }
  }

  def getState(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[Option[UUID]] = Future {
    withSQL {
      select(column.column("uuid")).from(SeqUpdate as upd)
        .where.eq(upd.column("auth_id"), authId)
        .orderBy(upd.column("uuid")).desc.limit(1)
    }.map(rs => new UUID(rs.string(column.column("uuid")))).single.apply
  }

  def getDifference(authId: Long, state: Option[UUID], limit: Int = 500)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[List[Entity[UUID, updateProto.SeqUpdateMessage]]] = Future {
    val sizedUpdates: List[UpdateWithSize] = withSQL {
      @inline
      def query(whereOpt: Option[SQLSyntax]): PagingSQLBuilder[UpdateWithSize] =
        select.from(SeqUpdate as upd)
          .where
          .eq(upd.column("auth_id"), authId)
          .and(whereOpt)
          .orderBy(upd.column("uuid")).asc.limit(limit)

      state match {
        case Some(uuid) =>
          query(Some(sqls.gt(upd.column("uuid"), sqls"CAST(${uuid.toString} as uuid)")))
        case None =>
          query(None)
      }
    }.map(SeqUpdate(upd)).list.apply

    @annotation.tailrec
    def collect(
      sizedUpdates: List[UpdateWithSize],
      updates: List[Entity[UUID, updateProto.SeqUpdateMessage]],
      size: Long
    ): List[Entity[UUID, updateProto.SeqUpdateMessage]] = {
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

  def push(authId: Long, update: updateProto.SeqUpdateMessage)(
    implicit ec: ExecutionContext, session: DBSession = SeqUpdate.autoSession
  ): Future[UUID] =
    push(new UUID, authId, update)

  def push(uuid: UUID, authId: Long, update: updateProto.SeqUpdateMessage)(
    implicit ec: ExecutionContext, session: DBSession
  ): Future[UUID] = Future {
    val (protobufData, header) = update match {
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

    withSQL {
      val x = column.column("protobuf_data")

      insert.into(SeqUpdate).namedValues(
        column.column("auth_id") -> authId,
        column.column("uuid") -> sqls"""CAST(${uuid.toString} as uuid)""",
        column.column("header") -> header,
        column.column("protobuf_data") -> protobufData.toByteArray
      )
    }.execute.apply

    uuid
  }
}
