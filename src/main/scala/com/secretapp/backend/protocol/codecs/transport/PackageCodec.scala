package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs.message.ProtoMessageWrapperCodec
import com.secretapp.backend.data._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

object PackageCodec extends Codec[Package] {

  private val codec = (int64 :: int64 :: ProtoMessageWrapperCodec).as[Package]

  def encode(p: Package) = codec.encode(p)

  def decode(buf: BitVector) = codec.decode(buf)

  def build(authId : Long, sessionId : Long, messageId : Long, body : ProtoMessage) = {
    encode(Package(authId, sessionId, ProtoMessageWrapper(messageId, body)))
  }

}
