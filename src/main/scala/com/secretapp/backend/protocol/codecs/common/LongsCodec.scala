package com.secretapp.backend.protocol.codecs.common

import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import com.secretapp.backend.protocol.codecs._

object LongsCodec extends Codec[Seq[Long]] {
  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(a: Seq[Long]) = {
    for {
      len <- varint.encode(a.length)
    } yield a.map(BitVector.fromLong(_)).foldLeft(len)(_ ++ _)
  }

  def decode(buf: BitVector) = {
    for {
      l <- varint.decode(buf); (xs, len) = l
    } yield {
      val bitsLen = len * longSize
      (xs.drop(bitsLen), xs.take(bitsLen).grouped(longSize).map(_.toLong()))
    }
  }
}
