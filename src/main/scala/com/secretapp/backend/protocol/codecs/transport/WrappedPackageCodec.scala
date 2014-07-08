package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs.utils._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

object WrappedPackageCodec extends Codec[WrappedPackage] {

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(wp: WrappedPackage) = {
    for {
      p <- protoPackage.encode(wp.p)
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
          } yield (remain, WrappedPackage(p))
        } else "invalid crc32".left
      case l@(-\/(e)) => l
    }
  }

  def encode(p: Package) : String \/ BitVector = encode(WrappedPackage(p))

}
