package com.secretapp.backend.api.counters

import akka.actor._
import akka.persistence._

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

  log.info(s"Starting counter $name with initial state = $initial")

  def receiveCommand: Actor.Receive = {
    case Get =>
      sender() ! count
    case GetNext =>
      val replyTo = sender()
      count += 1
      replyTo ! count
      saveSnapshot(count)
    case GetBulk(size) =>
      val replyTo = sender()
      val first = count + 1
      val last = count + size
      count = last
      replyTo ! Bulk(first, last)
      saveSnapshot(count)
    case s: SaveSnapshotSuccess =>
    case e: SaveSnapshotFailure =>
      log.error("SaveSnapshotFailure {}", e)
  }

  def receiveRecover: Actor.Receive = {
    case SnapshotOffer(metadata, offeredSnapshot) =>
      count = offeredSnapshot.asInstanceOf[StateType]
  }
}
