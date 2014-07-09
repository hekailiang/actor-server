package com.secretapp.backend.api

import akka.actor._
import scala.annotation.tailrec
import scala.collection.immutable

sealed trait AckTrackerMessage

case class RegisterSent[K, V](key: K, value: V) extends AckTrackerMessage

case class RegisterAck[K](key: K) extends AckTrackerMessage

// perhaps in future we will need it to be case class with key or key and value
case object AlreadyRegistered extends AckTrackerMessage

case object GetUnackd extends AckTrackerMessage

class AckTracker[K, V](val limit: Int) extends Actor with ActorLogging {
  import context._

  case class State(things: immutable.Map[K, V])

  def trackThings(state: State): Receive = {
    case m: RegisterSent[K, V] =>
      state.things.get(m.key) match {
        case Some(value) =>
          sender ! AlreadyRegistered
        case None =>
          become(trackThings(addNew(state, m.key, m.value)))
      }
    case m: RegisterAck[K] =>
      // TODO: drop things over limit
      become(trackThings(State(state.things - m.key)))
    case GetUnackd =>
      sender ! state.things
  }

  def receive = trackThings(State(immutable.Map[K, V]()))

  /**
    * Add new element
    */
  private def addNew(state: State, key: K, value: V): State = {
    State(state.things + Tuple2(key, value))
  }
}
