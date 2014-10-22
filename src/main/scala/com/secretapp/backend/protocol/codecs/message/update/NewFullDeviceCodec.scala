package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object NewFullDeviceCodec extends Codec[NewFullDevice] with utils.ProtobufCodec {
  def encode(n: NewFullDevice) = {
    val boxed = protobuf.UpdateNewFullDevice(n.uid, n.keyHash, n.key)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateNewFullDevice.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateNewFullDevice(uid, keyHash, key)) => NewFullDevice(uid, keyHash, key)
    }
  }
}
