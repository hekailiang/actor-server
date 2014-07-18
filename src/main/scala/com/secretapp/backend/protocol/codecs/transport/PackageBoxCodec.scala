package com.secretapp.backend.protocol.codecs.transport

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

object PackageBoxCodec extends Codec[PackageBox] {

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(pb: PackageBox) = {
    for {
      p <- protoPackage.encode(pb.p)
      len <- varint.encode(p.length / byteSize + crcByteSize)
      body = len ++ p
    } yield body ++ encodeCRCR32(body)
  }

  def decode(buf: BitVector) = {
    varint.decode(buf) match { // read varint length of Package: body + crc32
      case \/-((xs, len)) =>
        val bodyLen = (len - crcByteSize) * byteSize // get body size without crc32
        val body = xs.take(bodyLen)
        val crc = xs.drop(bodyLen).take(crcByteSize * byteSize)
        val varIntSize = varint.sizeOf(len) * byteSize // varint bit size
        val crcBody = buf.take(varIntSize + bodyLen) // crc body: package len + package body
        if (crc == encodeCRCR32(crcBody)) {
          for {
            pt <- protoPackage.decode(body) ; (_, p) = pt
            remain = buf.drop(varIntSize + len * byteSize)
          } yield (remain, PackageBox(p))
        } else "invalid crc32".left
      case l@(-\/(e)) => l
    }
  }

  def encode(p: Package): String \/ BitVector = encode(PackageBox(p))

  def build(authId: Long, sessionId: Long, messageId: Long, message: TransportMessage) = {
    encode(Package(authId, sessionId, MessageBox(messageId, message)))
  }

}
