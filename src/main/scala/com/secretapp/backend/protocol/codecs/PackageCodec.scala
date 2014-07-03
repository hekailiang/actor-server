package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import com.secretapp.backend.protocol._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import java.util.zip.CRC32
import scodec.bits._

trait PackageCodec {
  val packageHead: Codec[PackageHead] = (int64 :: int64 :: int64 :: int16).as[PackageHead]
}

object PackageCodec extends PackageCodec {
  def encode(authId: Long, sessionId: Long, messageId: Long, msg: ProtoMessage) = {
    for {
      m <- protoMessage.encode(msg)
      h <- packageHead.encode(PackageHead(authId, sessionId, messageId, (m.length / 8).toInt))
      payload = h ++ m
      len <- VarIntCodec.encode(payload.length / 8 + 8)
      body = len ++ payload
    } yield body ++ encodeCRCR32(body)
  }

  def decode(buf: BitVector): String \/ (BitVector, Package) = {
    val res = for {
      l <- VarIntCodec.decode(buf)
      h <- packageHead.decode(l._1)
      m <- protoMessage.decode(h._1)
    } yield {
      val takeLen = VarIntCodec.sizeOf(l._2) * 8 + l._2 * 8 - 64
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
