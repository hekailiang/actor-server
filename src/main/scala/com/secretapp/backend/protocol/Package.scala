package com.secretapp.backend.protocol

import codecs._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

case class PackageHead(authId: Long,
                       sessionId: Long,
                       messageId: Long,
                       messageLength: Int)
{
  val messageBitLength = messageLength * 8L
}
case class PackageMessage[T <: Struct](message: T)
case class Package[T <: PackageMessage[_]](head: PackageHead, message: T)

object Package {

  val codecHead: Codec[PackageHead] = (int64 :: int64 :: int64 :: int16).as[PackageHead]

  def headerSize = 8 * 3 + 16
  def headerBitSize = 64L * 3 + 16

}
