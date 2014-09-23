package com.secretapp.backend.protocol.codecs.message.update

import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object CommonUpdateMessageCodec {
  def encode(body: CommonUpdateMessage): String \/ BitVector = {
    body match {
      case m: Message => MessageCodec.encode(m)
      case m: MessageSent => MessageSentCodec.encode(m)
      case n: NewDevice => NewDeviceCodec.encode(n)
      case n: NewYourDevice => NewYourDeviceCodec.encode(n)
      case u: AvatarChanged => AvatarChangedCodec.encode(u)
    }
  }

  def decode(commonUpdateType: Int, buf: BitVector): String \/ CommonUpdateMessage = {
    val tried = Try(commonUpdateType match {
      case Message.commonUpdateType => MessageCodec.decode(buf)
      case MessageSent.commonUpdateType => MessageSentCodec.decode(buf)
      case NewDevice.commonUpdateType => NewDeviceCodec.decode(buf)
      case NewYourDevice.commonUpdateType => NewYourDeviceCodec.decode(buf)
      case AvatarChanged.commonUpdateType => AvatarChangedCodec.decode(buf)
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
