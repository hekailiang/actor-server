package com.secretapp.backend.services.transport

import akka.actor.Actor
import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import scala.annotation.tailrec
import scalaz._
import Scalaz._

trait WrappedPackageService extends PackageManagerService with PackageAckService { self: Actor =>
  import ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen: Long) extends ParseState

  type ParseResult = (ParseState, BitVector)
  type PackageFunc = (Package, Option[TransportMessage]) => Unit

  val minParseLength = varint.maxSize * byteSize // we need first 10 bytes for package size: package size varint (package + crc) + package + crc 32 int 32
  val maxPackageLen = (1024 * 1024 * 1.5).toLong // 1.5 MB

  @tailrec @inline
  private final def parseByteStream(state: ParseState, buf: BitVector)(f: PackageFunc): HandleError \/ ParseResult =
    state match {
      case sp@WrappedPackageSizeParsing() =>
        if (buf.length >= minParseLength) {
          varint.decode(buf) match {
            case \/-((_, len)) =>
              val pLen = (len + varint.sizeOf(len)) * byteSize // length of Package payload (with crc) + length of varint before Package
              if (len <= maxPackageLen) {
                parseByteStream(WrappedPackageParsing(pLen), buf)(f)
              } else {
                ParseError(s"received package size $len is bigger than $maxPackageLen bytes").left
              }
            case -\/(e) => ParseError(e).left
          }
        } else {
          (sp, buf).right
        }

      case pp@WrappedPackageParsing(bitsLen) =>
        if (buf.length >= bitsLen) {
          protoPackageBox.decode(buf) match {
            case \/-((remain, wp)) =>
              handlePackageAuthentication(wp.p)(f)
              log.info(s"remain: $remain, buf: $buf")
              parseByteStream(WrappedPackageSizeParsing(), remain)(f)
            case -\/(e) => ParseError(e).left
          }
        } else {
          (pp, buf).right
        }

      case _ => ParseError("internal error: wrong state").left
    }

  private var parseState: ParseState = WrappedPackageSizeParsing()
  private var parseBuffer = BitVector.empty

  /**
   * Parse bit stream, handle Package's and parsing failures.
   *
   * @param buf bit stream for parsing and handling
   * @param packageFunc handle Package function and maybe additional reply message
   * @param failureFunc handle parsing failures function
   */
  final def handleByteStream(buf: BitVector)(packageFunc: PackageFunc, failureFunc: (HandleError) => Unit): Unit = {
    parseByteStream(parseState, parseBuffer ++ buf)(packageFunc) match {
      case \/-((newState, remainBuf)) =>
        parseState = newState
        parseBuffer = remainBuf
      case -\/(e) =>
        log.error(s"handleByteStream#$parseState: $e")
        failureFunc(e)
    }
  }

  def replyPackage(p: Package): ByteString = {
    protoPackageBox.encode(p) match {
      case \/-(bv) =>
        val bs = ByteString(bv.toByteArray)
        registerSentMessage(p.messageBox, bs)
        bs
      case -\/(e) => ByteString(e)
    }
  }
}
