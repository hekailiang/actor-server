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

object NewYourDeviceCodec extends Codec[NewYourDevice] {
  def encode(n : NewYourDevice) = {
    val boxed = protobuf.UpdateNewYourDevice(n.uid, n.keyHash, n.key)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    Try(protobuf.UpdateNewYourDevice.parseFrom(buf.toByteArray)) match {
      case Success(protobuf.UpdateNewYourDevice(uid, keyHash, key)) =>
        (BitVector.empty, NewYourDevice(uid, keyHash, key)).right
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
