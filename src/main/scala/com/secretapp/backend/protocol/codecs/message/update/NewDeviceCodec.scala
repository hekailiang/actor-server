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

object NewDeviceCodec extends Codec[NewDevice] {
  def encode(n : NewDevice) = {
    val boxed = protobuf.UpdateNewDevice(n.uid, n.keyHash)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    Try(protobuf.UpdateNewDevice.parseFrom(buf.toByteArray)) match {
      case Success(protobuf.UpdateNewDevice(uid, keyHash)) =>
        (BitVector.empty, NewDevice(uid, keyHash)).right
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
