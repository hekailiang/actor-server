package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector

sealed trait Struct

trait StructSerializer[T] extends (T => ByteVector) { self =>
  def apply(message: T): ByteVector
}

trait StructDeserializer[T] extends (ByteVector => T) { self =>
  def apply(buf: ByteVector): T
}

object StructDeserializer {
  def apply[T <: Struct](buf: ByteVector)(implicit f: StructDeserializer[T]): T = f(buf)
}

object Struct {
  implicit object respAuthSerializer extends StructSerializer[ResponseAuth] {
    def apply(message: ResponseAuth): ByteVector = ByteVector(message.header) ++ Longs.encode(message.authId)
  }

  implicit object reqAuthDeserializer extends StructDeserializer[RequestAuth] {
    def apply(buf: ByteVector) = RequestAuth()
  }


  def encode[T <: Struct](message: T)(implicit f: StructSerializer[T]): ByteVector = f(message)

  def decode(buf: ByteVector) = buf(0) match {
    case 0xf0 => StructDeserializer[RequestAuth](buf)
  }
}

case class RequestAuth() extends Struct {
  val header: Byte = 0xf0.toByte
}

case class ResponseAuth(authId: Long) extends Struct {
  val header: Byte = 0xf1.toByte
}
