package com.secretapp.backend.protocol.codecs.utils

import net.sandrogrzicic.scalabuff.MessageBuilder

import scala.language.implicitConversions
import scala.collection.JavaConversions._
import scala.util.{ Try, Success, Failure }
import scodec.bits.BitVector
import com.google.protobuf.{ ByteString => ProtoByteString }
import scalaz._
import Scalaz._
import com.secretapp.backend.data.types

package object protobuf {
  implicit def bitVector2ProtoByteString(buf: BitVector): ProtoByteString = ProtoByteString.copyFrom(buf.toByteBuffer)
  implicit def optBitVector2ProtoByteString(buf: Option[BitVector]): Option[ProtoByteString] = {
    buf.flatMap(bitVector2ProtoByteString(_).some)
  }

  implicit def protoByteString2BitVector(bs: ProtoByteString): BitVector = BitVector(bs.toByteArray)
  implicit def optProtoByteString2OptBitVector(bs: Option[ProtoByteString]): Option[BitVector] = {
    bs.flatMap(protoByteString2BitVector(_).some)
  }

  def encodeToBitVector[T](mb: MessageBuilder[T]): String \/ BitVector = {
    Try(mb.toByteBuffer) match {
      case Success(buf) => BitVector(buf).right
      case Failure(e) => e.getMessage.left
    }
  }
}
