package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.secretapp.{ proto => protobuf }

object InitConnectionCodec extends Codec[InitConnection] with utils.ProtobufCodec {
  def encode(c: InitConnection) = {
    val boxed = protobuf.InitConnection(c.applicationId, c.applicationVersionIndex, c.deviceVendor, c.deviceModel,
      c.appLanguage, c.osLanguage, c.countryISO)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.InitConnection.parseFrom(buf.toByteArray)) {
      case Success(protobuf.InitConnection(applicationId, applicationVersionIndex, deviceVendor, deviceModel, appLanguage, osLanguage,
      countryISO)) =>
        InitConnection(applicationId, applicationVersionIndex, deviceVendor, deviceModel, appLanguage, osLanguage,
          countryISO)
    }
  }
}
