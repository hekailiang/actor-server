package com.secretapp.backend.protocol.types

sealed trait Struct

trait StructSerializer[T] extends (T => List[Byte]) { self =>
  def apply(message: T): List[Byte]
}

trait StructDeserializer[T] extends (List[Byte] => T) { self =>
  def apply(buf: List[Byte]): T
}

object StructDeserializer {
  def apply[T <: Struct](buf: List[Byte])(implicit f: StructDeserializer[T]): T = f(buf)
}

object Struct {
  implicit object respAuthSerializer extends StructSerializer[ResponseAuth] {
    def apply(message: ResponseAuth): List[Byte] = message.header :: Longs.encode(message.authId)
  }

  implicit object reqAuthDeserializer extends StructDeserializer[RequestAuth] {
    def apply(buf: List[Byte]) = RequestAuth()
  }


  def encode[T <: Struct](message: T)(implicit f: StructSerializer[T]): List[Byte] = f(message)

  def decode(buf: List[Byte]) = buf(0) match {
    case 0xf0 => StructDeserializer[RequestAuth](buf)
  }
}

case class RequestAuth() extends Struct {
  val header: Byte = 0xf0.toByte
}

case class ResponseAuth(authId: Long) extends Struct {
  val header: Byte = 0xf1.toByte
}
