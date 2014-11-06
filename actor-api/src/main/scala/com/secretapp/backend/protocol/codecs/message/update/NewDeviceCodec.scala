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

object NewDeviceCodec extends Codec[NewDevice] with utils.ProtobufCodec {
  def encode(n: NewDevice) = {
    val boxed = protobuf.UpdateNewDevice(n.uid, n.keyHash, n.key, n.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateNewDevice.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateNewDevice(uid, keyHash, key, date)) => NewDevice(uid, keyHash, key, date)
    }
  }
}
