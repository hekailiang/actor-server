package com.secretapp.backend.protocol.transport

import akka.actor.Actor
import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import scodec.codecs.int32
import scala.annotation.tailrec
import scalaz._
import Scalaz._

trait MTPackageService {
  self: Frontend =>
  import ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen: Long) extends ParseState

  type ParseResult = (ParseState, BitVector)
  type PackageFunc = MTPackage => Unit

  val minParseLength = intSize // we need first bytes for package size
  val maxPackageLen = (1024 * 1024 * 1.5).toLong // 1.5 MB

  @tailrec @inline
  private final def parseByteStream(state: ParseState, buf: BitVector)(f: PackageFunc): ParseError \/ ParseResult =
    state match {
      case sp@WrappedPackageSizeParsing() =>
        if (buf.length >= minParseLength) {
          int32.decode(buf) match {
            case \/-((_, len)) =>
              val pLen = (intSize / byteSize + len) * byteSize // length of Package length and length of Package payload (with index and crc)
              if (len <= maxPackageLen) {
                parseByteStream(WrappedPackageParsing(pLen), buf)(f)
              } else {
                ParseError(s"received package size $len is bigger than $maxPackageLen bytes").left
              }
            case -\/(e) => ParseError(e).left
          }
        } else (sp, buf).right
      case pp@WrappedPackageParsing(bitsLen) =>
        if (buf.length >= bitsLen) {
          protoPackageBox.decode(buf) match {
            case \/-((remain, wp)) =>
              // handlePackageAuthentication(wp.p)(f)
              f(wp.p)
              log.info(s"remain: $remain, buf: $buf")
              parseByteStream(WrappedPackageSizeParsing(), remain)(f)
            case -\/(e) => ParseError(e).left
          }
        } else (pp, buf).right
      case _ => ParseError("internal error: wrong state").left
    }

  private var parseState: ParseState = WrappedPackageSizeParsing()
  private var parseBuffer = BitVector.empty

  /**
   * Parse byte stream, handle Package's and parsing failures.
   *
   * @param buf byte stream for parsing and handling
   * @param packageFunc handle Package function and maybe additional reply message
   * @param failureFunc handle parsing failures function
   */
  final def handleByteStream(buf: BitVector)(packageFunc: PackageFunc, failureFunc: (ParseError) => Unit): Unit = {
    parseByteStream(parseState, parseBuffer ++ buf)(packageFunc) match {
      case \/-((newState, remainBuf)) =>
        parseState = newState
        parseBuffer = remainBuf
      case -\/(e) =>
        log.error(s"handleByteStream#$parseState: $e")
        failureFunc(e)
    }
  }
}
