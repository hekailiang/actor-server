package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object ProtoMessageWrapperCodec extends Codec[ProtoMessageWrapper] {

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(m: ProtoMessageWrapper) = {
    for {
      body <- protoMessage.encode(m.body)
      len <- varint.encode(body.length / byteSize)
    } yield (BitVector.fromLong(m.messageId) ++ len ++ body)
  }

  def decode(buf: BitVector) = {
    for {
      l <- varint.decode(buf.drop(longSize)); (xs, len) = l
      m <- protoMessage.decode(xs.take(len * byteSize)); (remain, msg) = m
    } yield {
      val messageId = buf.take(longSize).toLong()
      (xs.drop(len * byteSize), ProtoMessageWrapper(messageId, msg))
    }
  }

}
