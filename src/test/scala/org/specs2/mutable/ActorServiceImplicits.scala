package org.specs2.mutable

import akka.util.ByteString
import scala.language.implicitConversions
import scalaz._
import scalaz.Scalaz._
import scodec.bits._

trait ActorServiceImplicits {
  def codecRes2BS(res: String \/ BitVector): ByteString = ByteString(res.toOption.get.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2ByteVector(v: ByteString): ByteVector = ByteVector(v.toByteBuffer)
  implicit def bitVector2ByteString(v: BitVector): ByteString = ByteString(v.toByteBuffer)
  implicit def bitString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
}
