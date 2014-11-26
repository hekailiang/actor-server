package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.file.FastThumb
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.{models, proto}
import im.actor.messenger.{api => protobuf}
import scala.language.implicitConversions
import scalaz.Scalaz._

sealed trait MessageContent {
  val header: Int

  def toProto: protobuf.MessageContent

  def wrap(content: com.google.protobuf.GeneratedMessageLite): protobuf.MessageContent = {
    protobuf.MessageContent(header, content.toByteString)
  }
}

object MessageContent {
  def fromProto(m: protobuf.MessageContent): MessageContent = m.`type` match {
    case TextMessage.header => TextMessage.fromProto(protobuf.TextMessage.parseFrom(m.content))
    case ServiceMessage.header => ServiceMessage.fromProto(protobuf.ServiceMessage.parseFrom(m.content))
    case FileMessage.header => FileMessage.fromProto(protobuf.FileMessage.parseFrom(m.content))
  }
}

@SerialVersionUID(1L)
case class TextMessage(text: String) extends MessageContent {
  val header = TextMessage.header
  def toProto = super.wrap(protobuf.TextMessage(text))
}

object TextMessage {
  val header = 0x01

  def fromProto(m: protobuf.TextMessage) = TextMessage(m.text)
}

@SerialVersionUID(1L)
case class ServiceMessage(text: String, ext: Option[ServiceMessageExt]) extends MessageContent {
  val header = ServiceMessage.header

  def toProto = super.wrap(protobuf.ServiceMessage(text, ext.map(_.header).getOrElse(0), ext.map(_.toProto.toByteString)))
}

object ServiceMessage {
  val header = 0x02

  def fromProto(m: protobuf.ServiceMessage) = ServiceMessage(m.text, ServiceMessageExt.fromProto(m))
}

trait ServiceMessageExt {
  val header: Int

  def toProto: com.google.protobuf.GeneratedMessageLite
}

object ServiceMessageExt {
  def fromProto(m: protobuf.ServiceMessage): Option[ServiceMessageExt] = m.extType match {
    case UserAddedExtension.header => UserAddedExtension.fromProto(m).some
    case UserKickedExtension.header => UserKickedExtension.fromProto(m).some
    case UserLeftExtension.header => UserLeftExtension.fromProto(m).some
    case GroupCreatedExtension.header => GroupCreatedExtension.fromProto(m).some
    case GroupChangedTitleExtension.header => GroupChangedTitleExtension.fromProto(m).some
    case GroupChangedAvatarExtension.header => GroupChangedAvatarExtension.fromProto(m).some
    case _ => None
  }
}

@SerialVersionUID(1L)
case class UserAddedExtension(addedUid: Int) extends ServiceMessageExt {
  val header = UserAddedExtension.header

  def toProto = protobuf.ServiceExUserAdded(addedUid)
}

object UserAddedExtension {
  val header = 0x01

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceExUserAdded.parseFrom(m.ext.get)
    UserAddedExtension(ext.addedUid)
  }
}

@SerialVersionUID(1L)
case class UserKickedExtension(kickedUid: Int) extends ServiceMessageExt {
  val header =  UserKickedExtension.header

  def toProto = protobuf.ServiceExUserKicked(kickedUid)
}

object UserKickedExtension {
  val header = 0x02

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceExUserKicked.parseFrom(m.ext.get)
    UserKickedExtension(ext.kickedUid)
  }
}

@SerialVersionUID(1L)
case class UserLeftExtension() extends ServiceMessageExt {
  val header =  UserLeftExtension.header

  def toProto = protobuf.ServiceExUserLeft()
}

object UserLeftExtension {
  val header = 0x03

  def fromProto(m: protobuf.ServiceMessage) = UserLeftExtension()
}

@SerialVersionUID(1L)
case class GroupCreatedExtension() extends ServiceMessageExt {
  val header =  GroupCreatedExtension.header

  def toProto = protobuf.ServiceExGroupCreated()
}

object GroupCreatedExtension {
  val header = 0x04

  def fromProto(m: protobuf.ServiceMessage) = GroupCreatedExtension()
}

@SerialVersionUID(1L)
case class GroupChangedTitleExtension(title: String) extends ServiceMessageExt {
  val header =  GroupChangedTitleExtension.header

  def toProto = protobuf.ServiceExChangedTitle(title)
}

object GroupChangedTitleExtension {
  val header = 0x05

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceExChangedTitle.parseFrom(m.ext.get)
    GroupChangedTitleExtension(ext.title)
  }
}

@SerialVersionUID(1L)
case class GroupChangedAvatarExtension(avatar: Option[models.Avatar]) extends ServiceMessageExt {
  val header =  GroupChangedAvatarExtension.header

  def toProto = {
    protobuf.ServiceExChangedAvatar(avatar.map(proto.toProto[models.Avatar, protobuf.Avatar]))
  }
}

object GroupChangedAvatarExtension {
  val header = 0x06

  def fromProto(m: protobuf.ServiceMessage) = {
    val ext = protobuf.ServiceExChangedAvatar.parseFrom(m.ext.get)
    GroupChangedAvatarExtension(ext.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]))
  }
}

@SerialVersionUID(1L)
case class FileMessage(fileId: Long, accessHash: Long, fileSize: Int, name: String,
                       mimeType: String, thumb: Option[FastThumb], ext: Option[FileMessageExt]) extends MessageContent {
  val header = FileMessage.header

  def toProto = {
    super.wrap(
      protobuf.FileMessage(fileId, accessHash, fileSize, name, mimeType, thumb.map(_.toProto),
        ext.map(_.header).getOrElse(0), ext.map(_.toProto.toByteString)))
  }
}

object FileMessage {
  val header = 0x03

  def fromProto(m: protobuf.FileMessage) = {
    FileMessage(m.fileId, m.accessHash, m.fileSize, m.name, m.mimeType,
      m.thumb.map(FastThumb.fromProto), FileMessageExt.fromProto(m))
  }
}

trait FileMessageExt {
  val header: Int

  def toProto: com.google.protobuf.GeneratedMessageLite
}

object FileMessageExt {
  def fromProto(m: protobuf.FileMessage): Option[FileMessageExt] = m.extType match {
    case PhotoExtension.header => PhotoExtension.fromProto(m).some
    case VideoExtension.header => VideoExtension.fromProto(m).some
    case VoiceExtension.header => VoiceExtension.fromProto(m).some
    case _ => None
  }
}

@SerialVersionUID(1L)
case class PhotoExtension(w: Int, h: Int) extends FileMessageExt {
  val header =  PhotoExtension.header

  def toProto = protobuf.FileExPhoto(w, h)
}

object PhotoExtension {
  val header = 0x01

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileExPhoto.parseFrom(m.ext.get)
    PhotoExtension(ext.w, ext.h)
  }
}

@SerialVersionUID(1L)
case class VideoExtension(w: Int, h: Int, duration: Int) extends FileMessageExt {
  val header =  VideoExtension.header

  def toProto = protobuf.FileExVideo(w, h, duration)
}

object VideoExtension {
  val header = 0x02

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileExVideo.parseFrom(m.ext.get)
    VideoExtension(ext.w, ext.h, ext.duration)
  }
}

@SerialVersionUID(1L)
case class VoiceExtension(duration: Int) extends FileMessageExt {
  val header =  VoiceExtension.header

  def toProto = protobuf.FileExVoice(duration)
}

object VoiceExtension {
  val header = 0x03

  def fromProto(m: protobuf.FileMessage) = {
    val ext = protobuf.FileExVoice.parseFrom(m.ext.get)
    VoiceExtension(ext.duration)
  }
}
