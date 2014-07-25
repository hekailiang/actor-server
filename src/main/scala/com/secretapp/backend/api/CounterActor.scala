package com.secretapp.backend.api

import akka.actor._
import akka.persistence._

sealed case class Cmd(data: String)
sealed case class Evt(data: String)

object CounterProtocol {
  type StateType = Int

  sealed trait Request

  case object GetNext extends Request
  case class GetBulk(size: StateType) extends Request

  sealed trait Response
  case class Bulk(first: StateType, last: StateType) extends Response
}

class CounterActor(name: String, initial: CounterProtocol.StateType = 0) extends PersistentActor with ActorLogging {
  import CounterProtocol._

  override def persistenceId = s"counter-$name"

  var count = initial
  var lastSnapshottedAt: StateType = 0
  val minSnapshotStep: StateType  = 100

  def receiveCommand: Actor.Receive = {
    case GetNext =>
      val replyTo = sender
      persist(GetNext) { _ =>
        count += 1
        replyTo ! count
        maybeSnapshot()
      }
    case GetBulk(size) =>
      val replyTo = sender
      persist(GetBulk(size)) { _ =>
        val first = count + 1
        val last = count + size
        count = last
        replyTo ! Bulk(first, last)
        maybeSnapshot()
      }
    case s: SaveSnapshotSuccess =>
      log.debug("SaveSnapshotSuccess {}", s)
    case e: SaveSnapshotFailure =>
      log.error("SaveSnapshotFailure {}", e)
  }

  def receiveRecover: Actor.Receive = {
    case SnapshotOffer(metadata, offeredSnapshot) =>
      log.debug("SnapshotOffer {} {}", metadata, offeredSnapshot)
      count = offeredSnapshot.asInstanceOf[StateType]
    case GetNext =>
      count += 1
    case GetBulk(size) =>
      count += size
  }

  private def maybeSnapshot(): Unit = {
    if (count - lastSnapshottedAt >= minSnapshotStep) {
      log.debug("Saving snapshot count={} lastSnapshottedAt={}", count, lastSnapshottedAt)
      lastSnapshottedAt = count
      saveSnapshot(count)
    }
  }
}
