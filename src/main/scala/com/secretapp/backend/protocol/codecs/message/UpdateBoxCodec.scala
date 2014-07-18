package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object UpdateBoxCodec extends Codec[UpdateBox] {
  private val updateCodec: Codec[UpdateMessage] = discriminated[UpdateMessage].by(uint32)
    .\(Message.updateType) { case m: Message => m } (MessageCodec)
    .\(MessageSent.updateType) { case m: MessageSent => m } (MessageSentCodec)
    .\(NewDevice.updateType) { case n: NewDevice => n } (NewDeviceCodec)
    .\(NewYourDevice.updateType) { case n: NewYourDevice => n } (NewYourDeviceCodec)

  private val codec = protoPayload(updateCodec).pxmap[UpdateBox](UpdateBox.apply, UpdateBox.unapply)

  def encode(u: UpdateBox) = codec.encode(u)

  def decode(buf: BitVector) = codec.decode(buf)
}
