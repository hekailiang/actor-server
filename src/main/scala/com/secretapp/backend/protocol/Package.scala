package com.secretapp.backend.protocol

import codecs._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

case class Package(authId: Long,
                   sessionId: Long,
                   messageId: Long,
                   length: Int/*,
                   message: Struct*/)

object Package {

  implicit val codec: Codec[Package] = (int64L :: int64L :: int64L :: int16).as[Package]

}

//object Package {
//
//  def build[T <: Struct](authId: Long, sessionId: Long, messageId: Long, message: T)
//                        (implicit f: StructSerializer[T]): BitVector =
//  {
//    val msg = Struct.encode(message)
//    Longs.encode(authId) ++
//      Longs.encode(sessionId) ++
//      Longs.encode(messageId) ++
//      BitVector.fromInt(msg.length.toInt) ++
//      msg
//  }
//
//  def encode[T <: Struct](p: Package[T])(implicit f: StructSerializer[T]): BitVector = {
//    val message = Struct.encode(p.message)
//    Longs.encode(p.authId) ++
//      Longs.encode(p.sessionId) ++
//      Longs.encode(p.messageId) ++
//      BitVector.fromInt(message.length.toInt) ++
//      message
//  }
//
//  def decode[T <: Struct](buf: BitVector): Package[T] = ???
//
//}
