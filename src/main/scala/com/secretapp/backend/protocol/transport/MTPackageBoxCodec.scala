package com.secretapp.backend.protocol.transport

import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.codecs.utils._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.message.TransportMessage
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

object MTPackageBoxCodec extends Codec[MTPackageBox] {
  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(pb: MTPackageBox) = {
    for {
      p <- protoPackage.encode(pb.p)
      len <- int32.encode((intSize / byteSize + p.length / byteSize + crcByteSize).toInt)
      index <- int32.encode(pb.index)
      body = len ++ index ++ p
    } yield body ++ encodeCRCR32(body)
  }

  def encode(index: Int, p: BitVector) = {
    for {
      len <- int32.encode((intSize / byteSize + p.length / byteSize + crcByteSize).toInt)
      index <- int32.encode(index)
      body = len ++ index ++ p
    } yield body ++ encodeCRCR32(body)
  }

  def decode(buf: BitVector) = {
    (int32~int32).decode(buf) match { // read int length (index + body + crc32) and int index
      case \/-((xs, (len, index))) =>
        val bodyLen = (len - intSize / byteSize - crcByteSize) * byteSize // get body size without index and crc32
        val body = xs.take(bodyLen)
        val crc = xs.drop(bodyLen).take(crcByteSize * byteSize)
        val crcBody = buf.take(intSize + intSize + bodyLen) // crc body: package len + index + package body
        val expectedCrc = encodeCRCR32(crcBody)
        if (crc == expectedCrc) {
          for {
            pt <- protoPackage.decode(body) ; (_, p) = pt
            remain = buf.drop(intSize + len * byteSize)
          } yield (remain, MTPackageBox(index, p))
        } else s"invalid crc32 ${crc.toHex}, expected ${expectedCrc.toHex}".left
      case l@(-\/(e)) => l
    }
  }

  // TODO
  def build(index: Int, authId: Long, sessionId: Long, messageId: Long, message: TransportMessage) = {
    val mb = MessageBoxCodec.encodeValid(MessageBox(messageId, message))
    val blob = MTPackageCodec.encodeValid(MTPackage(authId, sessionId, mb))
    encode(index, blob)
  }
}
