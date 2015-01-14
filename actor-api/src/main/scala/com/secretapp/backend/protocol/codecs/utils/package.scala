package com.secretapp.backend.protocol.codecs

import java.util.zip.CRC32
import scala.util.{ Try, Success, Failure }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

package object utils {
  def encodeCRCR32(buf: BitVector): BitVector = {
    val crc32 = new CRC32()
    crc32.update(buf.toByteArray)
    BitVector.fromInt((crc32.getValue & 0xffffffff).toInt)
  }

  trait ProtobufCodec {
    def decodeProtobuf[T, R](res: => T)(f: PartialFunction[Success[T], R]): String \/ (BitVector, R) = {
      Try(res) match {
        case s@Success(_) => (BitVector.empty, f(s)).right
        case Failure(e) =>
          //e.printStackTrace()
          s"parse error: ${e.getMessage}".left
      }
    }

    def decodeProtobufEither[T, R](res: => T)(f: PartialFunction[Success[T], String \/ R]): String \/ (BitVector, R) = {
      Try(res) match {
        case s@Success(_) => f(s) match {
          case \/-(res) => (BitVector.empty, res).right
          case l@(-\/(_)) => l
        }
        case Failure(e) =>
//          e.printStackTrace()
          s"parse error: ${e.getMessage}".left
      }
    }
  }
}
