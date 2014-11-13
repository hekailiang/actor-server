package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.file.FastThumb
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.{models, proto}
import im.actor.messenger.{api => protobuf}

import scala.language.implicitConversions
import scalaz.Scalaz._

sealed trait MessageContent {
  val kind: Int

  def toProto: protobuf.MessageContent

  def wrap(content: com.google.protobuf.GeneratedMessageLite): protobuf.MessageContent = {
    protobuf.MessageContent(kind, content.toByteString)
  }
}

object MessageContent {
  def fromProto(m: protobuf.MessageContent) = m.`type` match {
    case TextMessage.header => TextMessage.fromProto(protobuf.TextMessage.parseFrom(m.content))
    case ServiceMessage.header => ServiceMessage.fromProto(protobuf.ServiceMessage.parseFrom(m.content))
    case FileMessage.header => FileMessage.fromProto(protobuf.FileMessage.parseFrom(m.content))
  }
}

@SerialVersionUID(1L)
case class TextMessage(text: String) extends MessageContent {
  val kind = TextMessage.header
  def toProto = super.wrap(protobuf.TextMessage(text))
}

object TextMessage {
  val header = 0x01

  def fromProto(m: protobuf.TextMessage) = TextMessage(m.text)
}

sealed trait ServiceMessage extends MessageContent {
  val kind = ServiceMessage.header
  val text: String
  val extType: Int

  override def wrap(content: com.google.protobuf.GeneratedMessageLite) = super.wrap(protobuf.ServiceMessage(text, extType, content.toByteString.some))
}

object ServiceMessage {
  val header = 0x02

  def fromProto(m: protobuf.ServiceMessage) = m.extType match {
    case UserAddedExtension.header => UserAddedExtension.fromProto(m)
    case UserKickedExtension.header => UserKickedExtension.fromProto(m)
    case UserLeftExtension.header => UserLeftExtension.fromProto(m)
    case GroupCreatedExtension.header => GroupCreatedExtension.fromProto(m)
    case GroupChangedTitleExtension.header => GroupChangedTitleExtension.fromProto(m)
    case GroupChangedAvatarExtension.header => GroupChangedAvatarExtension.fromProto(m)
  }
}

@SerialVersionUID(1L)
case class UserAddedExtension(text: String, addedUid: Int) extends ServiceMessage {
  val extType = UserAddedExtension.header
  def toProto = super.wrap(protobuf.ServiceMessage.UserAddedExtension(addedUid))
}

object UserAddedExtension {
  val header = 0x01

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceMessage.UserAddedExtension.parseFrom(m.ext.get)
    UserAddedExtension(m.text, ext.addedUid)
  }
}

@SerialVersionUID(1L)
case class UserKickedExtension(text: String, kickedUid: Int) extends ServiceMessage {
  val extType = UserKickedExtension.header
  def toProto = super.wrap(protobuf.ServiceMessage.UserKickedExtension(kickedUid))
}

object UserKickedExtension {
  val header = 0x02

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceMessage.UserKickedExtension.parseFrom(m.ext.get)
    UserKickedExtension(m.text, ext.kickedUid)
  }
}

@SerialVersionUID(1L)
case class UserLeftExtension(text: String) extends ServiceMessage {
  val extType = UserLeftExtension.header
  def toProto = super.wrap(protobuf.ServiceMessage.UserLeftExtension())
}

object UserLeftExtension {
  val header = 0x03

  def fromProto(m: protobuf.ServiceMessage) = {
    UserLeftExtension(m.text)
  }
}

@SerialVersionUID(1L)
case class GroupCreatedExtension(text: String) extends ServiceMessage {
  val extType = GroupCreatedExtension.header
  def toProto = super.wrap(protobuf.ServiceMessage.GroupCreatedExtension())
}

object GroupCreatedExtension {
  val header = 0x04

  def fromProto(m: protobuf.ServiceMessage) = {
    GroupCreatedExtension(m.text)
  }
}

@SerialVersionUID(1L)
case class GroupChangedTitleExtension(text: String, title: String) extends ServiceMessage {
  val extType = GroupChangedTitleExtension.header
  def toProto = super.wrap(protobuf.ServiceMessage.GroupChangedTitleExtension(title))
}

object GroupChangedTitleExtension {
  val header = 0x05

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceMessage.GroupChangedTitleExtension.parseFrom(m.ext.get)
    GroupChangedTitleExtension(m.text, ext.title)
  }
}

@SerialVersionUID(1L)
case class GroupChangedAvatarExtension(text: String, avatar: Option[models.Avatar]) extends ServiceMessage {
  val extType = GroupChangedAvatarExtension.header
  def toProto = {
    super.wrap(protobuf.ServiceMessage.GroupChangedAvatarExtension(
      avatar.map(proto.toProto[models.Avatar, protobuf.Avatar])))
  }
}

object GroupChangedAvatarExtension {
  val header = 0x06

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceMessage.GroupChangedAvatarExtension.parseFrom(m.ext.get)
    GroupChangedAvatarExtension(m.text, ext.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]))
  }
}


sealed trait FileMessage extends MessageContent {
  val kind = FileMessage.header
  val fileId: Int
  val accessHash: Long
  val fileSize: Int
  val name: String
  val mimeType: String
  val thumb: Option[FastThumb]
  val extType: Int
  def toProto(content: com.google.protobuf.GeneratedMessageLite) = {
    super.wrap(
      protobuf.FileMessage(
        fileId, accessHash, fileSize, name, mimeType, thumb.map(_.toProto), extType, content.toByteString.some))
  }
}

object FileMessage {
  val header = 0x03

  def fromProto(m: protobuf.FileMessage) = m.extType match {
    case PhotoExtension.header => PhotoExtension.fromProto(m)
    case VideoExtension.header => VideoExtension.fromProto(m)
    case VoiceExtension.header => VoiceExtension.fromProto(m)
  }
}


@SerialVersionUID(1L)
case class PhotoExtension(fileId: Int, accessHash: Long, fileSize: Int, name: String, mimeType: String, thumb: Option[FastThumb], w: Int, h: Int) extends FileMessage {
  val extType = PhotoExtension.header
  def toProto = super.wrap(protobuf.FileMessage.PhotoExtension(w, h))
}

object PhotoExtension {
  val header = 0x01

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileMessage.PhotoExtension.parseFrom(m.ext.get)
    PhotoExtension(m.fileId, m.accessHash, m.fileSize, m.name, m.mimeType, m.thumb.map(FastThumb.fromProto), ext.w, ext.h)
  }
}

@SerialVersionUID(1L)
case class VideoExtension(fileId: Int, accessHash: Long, fileSize: Int, name: String, mimeType: String, thumb: Option[FastThumb], w: Int, h: Int, duration: Int) extends FileMessage {
  val extType = VideoExtension.header
  def toProto = super.wrap(protobuf.FileMessage.VideoExtension(w, h, duration))
}

object VideoExtension {
  val header = 0x02

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileMessage.VideoExtension.parseFrom(m.ext.get)
    VideoExtension(m.fileId, m.accessHash, m.fileSize, m.name, m.mimeType, m.thumb.map(FastThumb.fromProto), ext.w, ext.h, ext.duration)
  }
}

@SerialVersionUID(1L)
case class VoiceExtension(fileId: Int, accessHash: Long, fileSize: Int, name: String, mimeType: String, thumb: Option[FastThumb], duration: Int) extends FileMessage {
  val extType = VoiceExtension.header
  def toProto = super.wrap(protobuf.FileMessage.VoiceExtension(duration))
}

object VoiceExtension {
  val header = 0x03

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileMessage.VoiceExtension.parseFrom(m.ext.get)
    VoiceExtension(m.fileId, m.accessHash, m.fileSize, m.name, m.mimeType, m.thumb.map(FastThumb.fromProto), ext.duration)
  }
}
