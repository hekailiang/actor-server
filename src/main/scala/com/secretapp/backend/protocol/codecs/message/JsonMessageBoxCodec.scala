package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.transport.MessageBox
import scodec.bits._
import scalaz._
import Scalaz._

object JsonMessageBoxCodec {
  def encode(mb: MessageBox): String \/ BitVector = {
    println(s"JsonMessageBoxCodec: $mb")
    // TODO
    BitVector(mb.body.toString.getBytes()).right
  }

  def encodeValid(mb: MessageBox): BitVector = encode(mb).toOption.get // TODO

  def decode(buf: BitVector): String \/ MessageBox = {
    println(s"decode JSON: $buf, ${new String(buf.toByteArray)}")
//    ???
    import com.secretapp.backend.data.message._
    MessageBox(1, Ping(10)).right
  }

  def decodeValue(buf: BitVector) = decode(buf) // TODO
}
