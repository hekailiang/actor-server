package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object UpdateBoxCodec extends Codec[UpdateBox] {
  private val updateCodec: Codec[UpdateMessage] = discriminated[UpdateMessage].by(uint8)
    .\(CommonUpdate.header) { case c : CommonUpdate => c } (CommonUpdateCodec)

  private val codec = updateCodec.pxmap[UpdateBox](UpdateBox.apply, UpdateBox.unapply)

  def encode(u : UpdateBox) = codec.encode(u)

  def decode(buf : BitVector) = codec.decode(buf)
}
