package com.secretapp.backend.protocol.codecs

sealed trait Struct

case class RequestAuth() extends Struct {
  val header: Byte = 0xf0.toByte
}

case class ResponseAuth(authId: Long) extends Struct {
  val header: Byte = 0xf1.toByte
}

case class Ping(randomId: Long) extends Struct {
  val header: Byte = 0x1.toByte
}

case class Pong(randomId: Long) extends Struct {
  val header: Byte = 0x2.toByte
}

case class Drop(messageId: Long, message: String) extends Struct {
  val header: Byte = 0xd.toByte
}
