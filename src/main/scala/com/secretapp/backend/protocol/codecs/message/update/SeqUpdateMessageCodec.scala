package com.secretapp.backend.protocol.codecs.message.update

import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.protocol.codecs.message.update.contact._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object SeqUpdateMessageCodec {
  def encode(body: SeqUpdateMessage): String \/ BitVector = {
    body match {
      case m: Message           => MessageCodec.encode(m)
      case m: MessageSent       => MessageSentCodec.encode(m)
      case n: NewDevice         => NewDeviceCodec.encode(n)
      case n: NewFullDevice     => NewFullDeviceCodec.encode(n)
      case n: RemoveDevice      => RemoveDeviceCodec.encode(n)
      case u: AvatarChanged     => AvatarChangedCodec.encode(u)
      case u: NameChanged       => NameChangedCodec.encode(u)
      case u: ContactRegistered => ContactRegisteredCodec.encode(u)
      case u: MessageReceived   => MessageReceivedCodec.encode(u)
      case u: MessageRead       => MessageReadCodec.encode(u)
      case u: GroupInvite       => GroupInviteCodec.encode(u)
      case u: GroupMessage      => GroupMessageCodec.encode(u)
      case u: GroupUserAdded    => GroupUserAddedCodec.encode(u)
      case u: GroupUserLeave    => GroupUserLeaveCodec.encode(u)
      case u: GroupUserKick     => GroupUserKickCodec.encode(u)
      case u: GroupCreated      => GroupCreatedCodec.encode(u)
      case u: GroupTitleChanged       => GroupTitleChangedCodec.encode(u)
      case u: GroupAvatarChanged      => GroupAvatarChangedCodec.encode(u)
      case u: UpdateContactRegistered => UpdateContactRegisteredCodec.encode(u)
      case u: UpdateContactsAdded     => UpdateContactsAddedCodec.encode(u)
      case u: UpdateContactsRemoved   => UpdateContactsRemovedCodec.encode(u)
    }
  }

  def decode(commonUpdateHeader: Int, buf: BitVector): String \/ SeqUpdateMessage = {
    val tried = Try(commonUpdateHeader match {
      case Message.header           => MessageCodec.decode(buf)
      case MessageSent.header       => MessageSentCodec.decode(buf)
      case NewDevice.header         => NewDeviceCodec.decode(buf)
      case NewFullDevice.header     => NewFullDeviceCodec.decode(buf)
      case RemoveDevice.header      => RemoveDeviceCodec.decode(buf)
      case AvatarChanged.header     => AvatarChangedCodec.decode(buf)
      case NameChanged.header       => NameChangedCodec.decode(buf)
      case ContactRegistered.header => ContactRegisteredCodec.decode(buf)
      case MessageReceived.header   => MessageReceivedCodec.decode(buf)
      case MessageRead.header       => MessageReadCodec.decode(buf)
      case GroupInvite.header       => GroupInviteCodec.decode(buf)
      case GroupMessage.header      => GroupMessageCodec.decode(buf)
      case GroupUserAdded.header    => GroupUserAddedCodec.decode(buf)
      case GroupUserLeave.header    => GroupUserLeaveCodec.decode(buf)
      case GroupUserKick.header     => GroupUserKickCodec.decode(buf)
      case GroupCreated.header      => GroupCreatedCodec.decode(buf)
      case GroupTitleChanged.header => GroupTitleChangedCodec.decode(buf)
      case GroupAvatarChanged.header=> GroupAvatarChangedCodec.decode(buf)
      case UpdateContactRegistered.header => UpdateContactRegisteredCodec.decode(buf)
      case UpdateContactsAdded.header     => UpdateContactsAddedCodec.decode(buf)
      case UpdateContactsRemoved.header   => UpdateContactsRemovedCodec.decode(buf)
    })
    tried match {
      case Success(res) => res match {
        case \/-(r) => r._2.right
        case l@(-\/(_)) => l
      }
      case Failure(e) => e.getMessage.left
    }
  }
}
