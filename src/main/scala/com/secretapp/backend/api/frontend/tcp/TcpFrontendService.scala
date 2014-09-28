//package com.secretapp.backend.api.frontend.tcp
//
//import akka.actor._
//import akka.io.Tcp._
//import akka.util.ByteString
//import com.secretapp.backend.data.message.TransportMessage
//import com.secretapp.backend.data.transport.{EncryptPackage, Package}
//import com.secretapp.backend.api.frontend.{SecurityFrontend, KeyFrontend}
//import com.secretapp.backend.protocol.codecs._
//import com.secretapp.backend.protocol.codecs.transport.TcpPackageCodec
//import scodec.bits.BitVector
//import scala.annotation.tailrec
//import scala.concurrent.duration._
//import scala.collection.{ immutable, mutable }
//import scalaz._
//import Scalaz._
//import com.datastax.driver.core.{ Session => CSession }
//
//object ClientFrontend {
//  sealed trait ClientFrontendMessage
//
//  @SerialVersionUID(1L)
//  case object SilentClose extends ClientFrontendMessage // with ControlMessage
//
//  @SerialVersionUID(1L)
//  case class ResponseToClient(payload: ByteString) extends ClientFrontendMessage
//
//  @SerialVersionUID(1L)
//  case class ResponseToClientWithDrop(payload: ByteString) extends ClientFrontendMessage
//}
//
//trait TcpFrontendService { this: Actor with ActorLogging =>
//  import context.system
//  import ByteConstants._
//  import ClientFrontend._
//  import KeyFrontend._
//
//  val connection: ActorRef
//  val sessionActor: ActorRef
//  implicit val csession: CSession
//
//  val minParseLength = varint.maxSize * byteSize // we need first 10 bytes for package size: package size varint (package + crc) + package + crc 32 int 32
//  val maxPackageLen = 1024L * 512L // 512 KB
//
//  sealed trait ParseState
//  case class WrappedPackageSizeParsing() extends ParseState
//  case class WrappedPackageParsing(bitsLen: Long) extends ParseState
//
//  type ParseResult = (ParseState, BitVector)
//
//  @tailrec @inline
//  final def parseByteStream(state: ParseState, buf: BitVector)(f: EncryptPackage => Unit): Option[ParseResult] =
//    state match {
//      case sp@WrappedPackageSizeParsing() =>
//        if (buf.length >= minParseLength) {
//          varint.decode(buf) match {
//            case \/-((_, len)) =>
//              val pLen = (len + varint.sizeOf(len)) * byteSize // length of Package payload (with crc) + length of varint before Package
//              if (len <= maxPackageLen) parseByteStream(WrappedPackageParsing(pLen), buf)(f)
//              else None
//            case -\/(e) => None
//          }
//        } else (sp, buf).some
//
//      case pp@WrappedPackageParsing(bitsLen) =>
//        if (buf.length >= bitsLen) {
//          TcpPackageCodec.decode(buf) match {
//            case \/-((remain, wp)) =>
//              f(wp.p)
//              log.info(s"remain: $remain, buf: $buf")
//              parseByteStream(WrappedPackageSizeParsing(), remain)(f)
//            case -\/(e) => None
//          }
//        } else (pp, buf).some
//
//      case _ => None
//    }
//
//  var parseState: ParseState = WrappedPackageSizeParsing()
//  var parseBuffer = BitVector.empty
//
//  def handleByteStream(buf: BitVector)(f: EncryptPackage => Unit): Unit = {
//    parseByteStream(parseState, parseBuffer ++ buf)(f) match {
//      case Some((newState, remainBuf)) =>
//        parseState = newState
//        parseBuffer = remainBuf
//      case None =>
//        log.error(s"handleByteStream#$parseState: None")
//        silentClose()
//    }
//  }
//
//  @inline
//  def silentClose(): Unit = {
//    log.info("SilentClose")
//    connection ! Close
//    context stop self
//  }
//
//  lazy val keyFrontend = context.system.actorOf(KeyFrontend.props(self)(csession), name = "key-frontend")
//  val secFrontendMap = mutable.Map[Long, ActorRef]()
//
//  def handleEncryptPackage(p: EncryptPackage): Unit = {
//    if (p.authId == 0L) keyFrontend ! InitDH(p)
//    else secFrontendMap.get(p.authId) match {
//      case Some(secFrontend) => secFrontend ! p
//      case None =>
//        val secFrontend = context.system.actorOf(SecurityFrontend.props(self, sessionActor, p.authId)(csession),
//          name = s"security-frontend_${p.authId}")
//        secFrontendMap += Tuple2(p.authId, secFrontend)
//        secFrontend ! p
//    }
//  }
//
//  def receiveTcpFrontend: PartialFunction[Any, Unit] = {
//    case Received(data) =>
//      log.info(s"Received: $data ${data.length}")
//      handleByteStream(BitVector(data.toArray))(handleEncryptPackage)
//
//    case ResponseToClient(payload) =>
//      log.info(s"Send to client: $payload")
//      connection ! Write(payload)
//
//    case ResponseToClientWithDrop(payload) =>
//      connection ! Write(payload)
//      silentClose()
//
//    case SilentClose =>
//      silentClose()
//
//    case PeerClosed | ErrorClosed | Closed =>
//      log.info(s"Connection closed")
//      context stop self
//  }
//}
//
//object TcpFrontend {
//  def props(connection: ActorRef, sessionActor: ActorRef)(implicit csession: CSession) = {
//    Props(new TcpFrontend(connection, sessionActor))
//  }
//}
//
//class TcpFrontend(val connection: ActorRef, val sessionActor: ActorRef)(implicit val csession: CSession) extends Actor with ActorLogging with TcpFrontendService {
//  context.setReceiveTimeout(30.minutes)
//
//  def receive = receiveTcpFrontend
//}
