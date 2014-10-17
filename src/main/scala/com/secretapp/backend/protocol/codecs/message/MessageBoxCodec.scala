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
      body <- protoPayload(protoTransportMessage).encode(mb.body)
    } yield BitVector.fromLong(mb.messageId) ++ body
  }

  def decode(buf: BitVector) = {
    for {
      m <- protoPayload(protoTransportMessage).decode(buf.drop(longSize)); (remain, msg) = m
    } yield {
      val messageId = buf.take(longSize).toLong()
      (remain, MessageBox(messageId, msg))
    }
  }
}
