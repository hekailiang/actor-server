package com.secretapp.backend.api.frontend

import akka.actor._
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

trait NackActor { this: Actor with ActorLogging =>
  import Tcp._
  import context.system

  val connection: ActorRef
  val remote: InetSocketAddress

  case class Ack(offset: Int) extends Event

  override def receive = writing

  def receiveBusinessLogic(writing: Boolean): Receive

  def send(data: ByteString, writing: Boolean): Unit = {
    if (writing) connection ! Write(data, Ack(currentOffset))
    buffer(data)
  }

  def writing: Receive = receiveBusinessLogic(writing = true) orElse {
    case Ack(ack) =>
      acknowledge(ack)

    case CommandFailed(Write(_, Ack(ack))) =>
      connection ! ResumeWriting
      context become buffering(ack)

    case PeerClosed | ErrorClosed | Closed =>
      if (storage.isEmpty) context stop self
      else context become closing
  }

  def buffering(nack: Int): Receive = receiveBusinessLogic(writing = false) orElse {
    var toAck = 10
    var peerClosed = false

    {
      case WritingResumed         => writeFirst()
      case PeerClosed | ErrorClosed | Closed => peerClosed = true
      case Ack(ack) if ack < nack => acknowledge(ack)
      case Ack(ack) =>
        acknowledge(ack)
        if (storage.nonEmpty) {
          if (toAck > 0) {
            // stay in ACK-based mode for a while
            writeFirst()
            toAck -= 1
          } else {
            // then return to NACK-based again
            writeAll()
            context become (if (peerClosed) closing else writing)
          }
        } else if (peerClosed) context stop self
        else context become writing
    }
  }

  def closing: Receive = {
    case CommandFailed(_: Write) =>
      connection ! ResumeWriting
      context.become({

        case WritingResumed =>
          writeAll()
          context.unbecome()

        case ack: Int => acknowledge(ack)

      }, discardOld = false)

    case Ack(ack) =>
      acknowledge(ack)
      if (storage.isEmpty) context stop self
  }

  override def postStop(): Unit = {
    log.info(s"transferred $transferred bytes from/to [$remote]")
  }

  private var storageOffset = 0
  private var storage = Vector.empty[ByteString]
  private var stored = 0L
  private var transferred = 0L

  val maxStored = 2 * 1024 * 1024 // 2 MB
  val highWatermark = maxStored * 5 / 10
  val lowWatermark = maxStored * 3 / 10
  private var suspended = false

  private def currentOffset = storageOffset + storage.size

  private def buffer(data: ByteString): Unit = {
    storage :+= data
    stored += data.size

    if (stored > maxStored) {
      log.warning(s"drop connection to [$remote] (buffer overrun)")
      context stop self

    } else if (stored > highWatermark) {
      log.debug(s"suspending reading at $currentOffset")
      connection ! SuspendReading
      suspended = true
    }
  }

  private def acknowledge(ack: Int): Unit = {
    require(ack == storageOffset, s"received ack $ack at $storageOffset")
    require(storage.nonEmpty, s"storage was empty at ack $ack")

    val size = storage(0).size
    stored -= size
    transferred += size

    storageOffset += 1
    storage = storage drop 1

    if (suspended && stored < lowWatermark) {
      log.debug("resuming reading")
      connection ! ResumeReading
      suspended = false
    }
  }

  private def writeFirst(): Unit = {
    connection ! Write(storage(0), Ack(storageOffset))
  }

  private def writeAll(): Unit = {
    for ((data, i) <- storage.zipWithIndex) {
      connection ! Write(data, Ack(storageOffset + i))
    }
  }
}
