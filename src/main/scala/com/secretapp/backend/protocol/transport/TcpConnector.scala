package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.{ ByteString, Timeout }
import com.secretapp.backend.data.transport.MessageBox
import scodec.bits._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import PackageCommon._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

class TcpConnector(val connection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends Connector with ActorLogging with WrappedPackageService with PackageService {
  import akka.io.Tcp._
  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5.seconds)

  var packageIndex = 0
  var closing = false

  var storage = Vector.empty[(Int, ByteString)]
  var stored = 0L
  var transferred = 0L

  val maxStored = 100000000L // TODO: think about this value
  val highWatermark = maxStored * 5 / 10
  val lowWatermark = maxStored * 3 / 10

  var suspended = false

  val handleActor = self

  context watch connection

  def nextPackage(p: MTPackage)(f: (Int, ByteString) => Unit): Unit = {
    val data = replyPackage(packageIndex, p)
    f(packageIndex, data)
    packageIndex = packageIndex + 1
  }

  def receive = writing

  def writing: Receive = {
    case pe: PackageEither =>
      wlog(pe)
      pe match {
        case \/-(p) =>
          nextPackage(p) { (index, data) =>
            buffer(index, data)
            write(index, data)
          }
        case -\/(p) =>
          nextPackage(p) { (index, data) =>
            buffer(index, data)
            write(index, data)
          }
          connection ! Close
      }

    case Received(data) =>
      wlog(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      wlog("Connection closed by peer")
      context stop self

    case CommandFailed(x) =>
      wlog(s"CommandFailed ${x}")

    case PackageAck(index) =>
      throw new Exception("Received ack in writing mode")

    case StopConnector(p) =>
      nextPackage(p) { (index, data) =>
        buffer(index, data)
        write(index, data)
      }

      connection ! Close
  }

  def buffering: Receive = {
    case pe: PackageEither =>
      blog(s"PackageToSend($pe)")
      pe match {
        case \/-(p) =>
          nextPackage(p) { (index, data) =>
            buffer(index, data)
          }
        case -\/(p) =>
          nextPackage(p) { (index, data) =>
            buffer(index, data)
          }
          closing = true
      }
    case Received(data) =>
      blog(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      closing = true

    case CommandFailed(x) =>
      blog(s"CommandFailed ${x}")

    case PackageAck(index) =>
      blog(s"Ack ${index}")
      acknowledge()

    case StopConnector(p) =>
      nextPackage(p) { (index, data) =>
        buffer(index, data)
        write(index, data)
      }

      connection ! Close
  }

  private def write(index: Int, data: ByteString, becomeBuffering: Boolean = true): Unit = {
    log.debug(s"Sending ${connection} ${data} ${index}")
    connection ! Write(data, PackageAck(index))
    if (becomeBuffering) {
      context.become(buffering, discardOld = false)
    }
  }

  private def buffer(packageIndex: Int, data: ByteString): Unit = {
    storage :+= (packageIndex, data)
    stored += data.size

    if (stored > maxStored) {
      log.warning(s"drop connection to [connection] (buffer overrun)")
      context stop self

    } else if (stored > highWatermark) {
      log.debug(s"suspending reading")
      connection ! SuspendReading
      suspended = true
    }
  }

  private def acknowledge(): Unit = {
    require(storage.nonEmpty, "storage was empty")

    val size = storage(0)._2.size
    stored -= size
    transferred += size

    storage = storage drop 1

    if (suspended && stored < lowWatermark) {
      log.debug("resuming reading")
      connection ! ResumeReading
      suspended = false
    }

    if (storage.isEmpty) {
      if (closing) {
        log.debug("stopping")
        context stop self
      } else {
        log.debug("resuming writing")
        context.unbecome()
      }
    } else {
      val (index, data) = storage(0)
      write(index, data, false)
    }
  }

  def wlog(obj: Object) = log.info(s"[writing] ${logstr(obj)}")
  def blog(obj: Object) = log.info(s"[buffering] ${logstr(obj)}")

  @inline
  private def logstr(o: Object) = {
    o match {
      case s: String => s
      case _ => o
    }
  }
}
