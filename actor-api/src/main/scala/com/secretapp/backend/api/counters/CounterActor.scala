package com.secretapp.backend.api.counters

import akka.actor._
import akka.persistence._

sealed case class Cmd(data: String)
sealed case class Evt(data: String)

object CounterProtocol {
  type StateType = Int

  sealed trait Request

  case object GetNext extends Request
  case class GetBulk(size: StateType) extends Request
  case object Get extends Request

  sealed trait Response
  case class Bulk(first: StateType, last: StateType) extends Response
}

class CounterActor(name: String, initial: CounterProtocol.StateType = 0) extends PersistentActor with ActorLogging {
  import CounterProtocol._

  override def persistenceId = s"counter-$name"

  var count = initial
  var lastSnapshottedAt: StateType = 0
  val minSnapshotStep: StateType  = 100

  log.info(s"Starting counter $name with initial state = $initial")

  def receiveCommand: Actor.Receive = {
    case Get =>
      sender() ! count
    case GetNext =>
      val replyTo = sender()
      persist(GetNext) { _ =>
        count += 1
        replyTo ! count
        maybeSnapshot()
      }
    case GetBulk(size) =>
      val replyTo = sender()
      persist(GetBulk(size)) { _ =>
        val first = count + 1
        val last = count + size
        count = last
        replyTo ! Bulk(first, last)
        maybeSnapshot()
      }
    case s: SaveSnapshotSuccess =>
    case e: SaveSnapshotFailure =>
      log.error("SaveSnapshotFailure {}", e)
  }

  def receiveRecover: Actor.Receive = {
    case SnapshotOffer(metadata, offeredSnapshot) =>
      count = offeredSnapshot.asInstanceOf[StateType]
    case GetNext =>
      count += 1
    case GetBulk(size) =>
      count += size
  }

  private def maybeSnapshot(): Unit = {
    if (count - lastSnapshottedAt >= minSnapshotStep) {
      lastSnapshottedAt = count
      saveSnapshot(count)
    }
  }
}
