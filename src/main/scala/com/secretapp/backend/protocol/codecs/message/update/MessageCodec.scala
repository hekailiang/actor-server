package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.{ Try, Success, Failure }
import com.getsecretapp.{ proto => protobuf }

object MessageCodec extends Codec[Message] {
  def encode(m : Message) = {
    val boxed = protobuf.UpdateMessage(m.senderUID, m.destUID, m.mid, m.keyHash, m.useAesKey, m.aesKey, m.message)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    Try(protobuf.UpdateMessage.parseFrom(buf.toByteArray)) match {
      case Success(protobuf.UpdateMessage(senderUID, destUID, mid, keyHash, useAesKey, aesKey, message)) =>
        (BitVector.empty, Message(senderUID, destUID, mid, keyHash, useAesKey, aesKey, message)).right
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
