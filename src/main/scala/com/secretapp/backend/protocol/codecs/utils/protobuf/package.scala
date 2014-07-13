package com.secretapp.backend.protocol.codecs.utils

import net.sandrogrzicic.scalabuff.MessageBuilder

import scala.language.implicitConversions
import scala.collection.JavaConversions._
import scala.util.{ Try, Success, Failure }
import scodec.bits.BitVector
import com.google.protobuf.{ ByteString => ProtoByteString }
import scalaz._
import Scalaz._

package object protobuf {
  implicit def array2ProtoByteString(arr : Array[Byte]) : ProtoByteString = ProtoByteString.copyFrom(arr)
  implicit def list2ProtoByteString(list : List[Byte]) : ProtoByteString = array2ProtoByteString(list.toArray)

  implicit def protoByteString2Array(bs : ProtoByteString) : Array[Byte] = bs.toByteArray
  implicit def protoByteString2List(bs : ProtoByteString) : List[Byte] = bs.toByteArray.toList

  def encodeToBitVector[T](mb : MessageBuilder[T]) : String \/ BitVector = {
    Try(mb.toByteBuffer) match {
      case Success(buf) => BitVector(buf).right
      case Failure(e) => e.getMessage.left
    }
  }
}
