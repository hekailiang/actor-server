package com.secretapp.backend.api

import akka.actor._
import akka.persistence._

sealed case class Cmd(data: String)
sealed case class Evt(data: String)

case class CounterState(counter: Long) {
  def incremented: CounterState = copy(counter + 1)
}

object CounterProtocol {
  case object GetNext
}

class CounterActor(name: String, initial: Int = 0) extends PersistentActor {
  override def persistenceId = s"counter-$name"

  var state = CounterState(initial)

  def receiveCommand: Actor.Receive = {
    case CounterProtocol.GetNext =>
      val replyTo = sender
      persist(CounterProtocol.GetNext) { _ =>
        state = state.incremented
        replyTo ! state
      }
  }

  def receiveRecover: Actor.Receive = {
    case CounterProtocol.GetNext =>
      state = state.incremented
  }
}
