package com.secretapp.backend.protocol.codecs.message

import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object UpdateMessageCodec {
  def encode(body: UpdateMessage): String \/ BitVector = {
    body match {
      case m: Message => MessageCodec.encode(m)
      case m: MessageSent => MessageSentCodec.encode(m)
      case n: NewDevice => NewDeviceCodec.encode(n)
      case n: NewYourDevice => NewYourDeviceCodec.encode(n)
    }
  }

  def decode(updateType: Int, buf: BitVector): String \/ UpdateMessage = {
    val tryed = Try(updateType match {
      case Message.updateType => MessageCodec.decode(buf)
      case MessageSent.updateType => MessageSentCodec.decode(buf)
      case NewDevice.updateType => NewDeviceCodec.decode(buf)
      case NewYourDevice.updateType => NewYourDeviceCodec.decode(buf)
    })
    tryed match {
      case Success(res) => res match {
        case \/-(r) => r._2.right
        case l@(-\/(_)) => l
      }
      case Failure(e) => e.getMessage.left
    }
  }
}
