package com.secretapp.backend.protocol

import types._
import scodec.bits._

case class Package[T <: Struct](authId: Long,
                                sessionId: Long,
                                messageId: Long,
                                length: Int,
                                message: T)

object Package {

  def build[T <: Struct](authId: Long, sessionId: Long, messageId: Long, message: T)
                        (implicit f: StructSerializer[T]): ByteVector =
  {
    val msg = Struct.encode(message)
    Longs.encode(authId) ++
      Longs.encode(sessionId) ++
      Longs.encode(messageId) ++
      BitVector.fromInt(msg.length, 32, ByteOrdering.BigEndian).bytes ++
      msg
  }

  def encode[T <: Struct](p: Package[T])(implicit f: StructSerializer[T]): ByteVector = {
    val message = Struct.encode(p.message)
    Longs.encode(p.authId) ++
      Longs.encode(p.sessionId) ++
      Longs.encode(p.messageId) ++
      BitVector.fromInt(message.length, 32, ByteOrdering.BigEndian).bytes ++
      message
  }

  def decode[T <: Struct](buf: ByteVector): Package[T] = ???

}
