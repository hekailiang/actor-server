package com.secretapp.backend.protocol

import codecs._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import java.util.zip.CRC32

case class PackageHead(authId: Long,
                       sessionId: Long,
                       messageId: Long,
                       messageLength: Int)
{
  val messageBitLength = messageLength * 8L
}
case class PackageMessage(message: Message)
case class Package(head: PackageHead, message: Message)

object Package {

  val codecHead: Codec[PackageHead] = (int64 :: int64 :: int64 :: int16).as[PackageHead]

  def encode(authId: Long, sessionId: Long, messageId: Long, msg: Message) = {
    for {
      m <- Message.codec.encode(msg)
      h <- codecHead.encode(PackageHead(authId, sessionId, messageId, (m.length / 8).toInt))
      payload = h ++ m
      len <- VarInt.encode(payload.length / 8 + 8)
      body = len ++ payload
    } yield body ++ encodeCRCR32(body)
  }

  def decode(buf: BitVector): String \/ (BitVector, Package) = {
    val res = for {
      l <- VarInt.decode(buf)
      h <- codecHead.decode(l._1)
      m <- Message.codec.decode(h._1)
    } yield {
      val takeLen = VarInt.sizeOf(l._2) * 8 + l._2 * 8 - 64
      val p = buf.take(takeLen)
      (m._1 == encodeCRCR32(p), buf.drop(takeLen + 64), Package(h._2, m._2))
    }
    res match {
      case \/-(((valid, remain, p))) =>
        if (valid) (remain, p).right
        else "invalid crc32".left
      case l@(-\/(e)) => l
    }
  }

  def encodeCRCR32(buf: BitVector): BitVector = {
    val crc32 = new CRC32()
    crc32.update(buf.toByteArray)
    BitVector.fromLong(crc32.getValue)
  }

}
