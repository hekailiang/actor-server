package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object MessageBoxCodec extends Codec[MessageBox] {
  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(mb: MessageBox) = {
    for {
      body <- protoTransportMessage.encode(mb.body)
      len <- varint.encode(body.length / byteSize)
    } yield (BitVector.fromLong(mb.messageId) ++ len ++ body)
  }

  def decode(buf: BitVector) = {
    for {
      l <- varint.decode(buf.drop(longSize)); (xs, len) = l
      m <- protoTransportMessage.decode(xs.take(len * byteSize)); (remain, msg) = m
    } yield {
      val messageId = buf.take(longSize).toLong()
      (xs.drop(len * byteSize), MessageBox(messageId, msg))
    }
  }
}
