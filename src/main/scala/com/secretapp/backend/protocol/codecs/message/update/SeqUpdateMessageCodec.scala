package com.secretapp.backend.protocol.codecs.message.update

import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.data.message.update._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object SeqUpdateMessageCodec {
  def encode(body: SeqUpdateMessage): String \/ BitVector = {
    body match {
      case m: Message           => MessageCodec.encode(m)
      case m: MessageSent       => MessageSentCodec.encode(m)
      case n: NewDevice         => NewDeviceCodec.encode(n)
      case n: NewYourDevice     => NewYourDeviceCodec.encode(n)
      case u: AvatarChanged     => AvatarChangedCodec.encode(u)
      case u: ContactRegistered => ContactRegisteredCodec.encode(u)
      case u: MessageReceived   => MessageReceivedCodec.encode(u)
      case u: MessageRead       => MessageReadCodec.encode(u)
    }
  }

  def decode(commonUpdateHeader: Int, buf: BitVector): String \/ SeqUpdateMessage = {
    val tried = Try(commonUpdateHeader match {
      case Message.seqUpdateHeader           => MessageCodec.decode(buf)
      case MessageSent.seqUpdateHeader       => MessageSentCodec.decode(buf)
      case NewDevice.seqUpdateHeader         => NewDeviceCodec.decode(buf)
      case NewYourDevice.seqUpdateHeader     => NewYourDeviceCodec.decode(buf)
      case AvatarChanged.seqUpdateHeader     => AvatarChangedCodec.decode(buf)
      case ContactRegistered.seqUpdateHeader => ContactRegisteredCodec.decode(buf)
      case MessageReceived.seqUpdateHeader   => MessageReceivedCodec.decode(buf)
      case MessageRead.seqUpdateHeader       => MessageReadCodec.decode(buf)
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
