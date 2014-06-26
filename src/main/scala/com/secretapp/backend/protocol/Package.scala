package com.secretapp.backend.protocol

import types._
import com.google.common.primitives.Ints

case class Package[T <: Struct](authId: Long,
                                sessionId: Long,
                                messageId: Long,
                                length: Int,
                                message: T)

object Package {

  def build[T <: Struct](authId: Long, sessionId: Long, messageId: Long, message: T)
                        (implicit f: StructSerializer[T]): List[Byte] =
  {
    val msg = Struct.encode(message)
    Longs.encode(authId) ++
      Longs.encode(sessionId) ++
      Longs.encode(messageId) ++
      Ints.toByteArray(msg.length) ++
      msg
  }

  def encode[T <: Struct](p: Package[T])(implicit f: StructSerializer[T]): List[Byte] = {
    val message = Struct.encode(p.message)
    Longs.encode(p.authId) ++
      Longs.encode(p.sessionId) ++
      Longs.encode(p.messageId) ++
      Ints.toByteArray(message.length) ++
      message
  }

  def decode[T <: Struct](buf: List[Byte]): Package[T] = ???

}
