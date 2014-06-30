package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._


sealed trait Struct // deprecate it?
sealed trait Message extends Struct

case class RequestAuthId() extends Message
object RequestAuthId {
  val header = 0xf0
  val codec = provide(RequestAuthId())
}

case class ResponseAuthId(authId: Long) extends Message
object ResponseAuthId {
  val header = 0xf1
  val codec = int64.pxmap[ResponseAuthId](ResponseAuthId.apply, ResponseAuthId.unapply)
}

case class Ping(randomId: Long) extends Message
object Ping {
  val header = 0x1
  val codec = int64.pxmap[Ping](Ping.apply, Ping.unapply)
}

case class Pong(randomId: Long) extends Message
object Pong {
  val header = 0x2
  val codec = int64.pxmap[Pong](Pong.apply, Pong.unapply)
}

case class NewSession(sessionId: Long, messageId: Long) extends Message
object NewSession {
  val header = 0xc
  val codec = (int64 ~ int64).pxmap[NewSession](NewSession.apply, NewSession.unapply)
}

case class Drop(messageId: Long, message: String) extends Message
object Drop {
  val header = 0xd
  val codec = (int64 ~ String.codec).pxmap[Drop](Drop.apply, Drop.unapply)
}

object Message {
  val codec: Codec[Message] = discriminated[Message].by(uint8)
    .\(RequestAuthId.header) { case ra@RequestAuthId() => ra}(RequestAuthId.codec)
    .\(ResponseAuthId.header) { case ra@ResponseAuthId(_) => ra}(ResponseAuthId.codec)
    .\(Ping.header) { case p@Ping(_) => p}(Ping.codec)
    .\(Pong.header) { case p@Pong(_) => p}(Pong.codec)
    .\(Drop.header) { case d@Drop(_, _) => d}(Drop.codec)
    .\(NewSession.header) { case s@NewSession(_, _) => s}(NewSession.codec)
}
