package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.protocol.codecs.utils._
import com.secretapp.backend.data._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

trait WrappedPackageCodec {

  val wrappedPackage: Codec[WrappedPackage] = new Codec[WrappedPackage] {

    import ByteConstants._

    def encode(wp: WrappedPackage) = {
      for {
        p <- packageCodec.encode(wp.p)
        len <- VarIntCodec.encode(p.length / byteSize + crcByteSize)
        body = len ++ p
      } yield body ++ encodeCRCR32(body)
    }

    def decode(buf: BitVector) = {
      VarIntCodec.decode(buf) match { // read varint length of Package: body + crc32
        case \/-((xs, len)) =>
          val bodyLen = (len - crcByteSize) * byteSize // get body size without crc32
          val body = xs.take(bodyLen)
          val crc = xs.drop(bodyLen).take(crcByteSize * byteSize)
          val varIntSize = VarIntCodec.sizeOf(len) * byteSize // varint bit size
          val crcBody = buf.take(varIntSize + bodyLen) // crc body: package len + package body
          if (crc == encodeCRCR32(crcBody)) {
            for {
              pt <- packageCodec.decode(body) ; (_, p) = pt
              remain = buf.drop(varIntSize + len * byteSize)
            } yield (remain, WrappedPackage(p))
          } else "invalid crc32".left
        case l@(-\/(e)) => l
      }
    }

  }

}

object WrappedPackageCodec extends WrappedPackageCodec {
  def encode(wp: WrappedPackage) = wrappedPackage.encode(wp)
  def decode(buf: BitVector) = wrappedPackage.decode(buf)
}
