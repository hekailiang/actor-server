package com.secretapp.backend.api

import akka.actor._
import akka.persistence._

sealed case class Cmd(data: String)
sealed case class Evt(data: String)

object CounterProtocol {
  case object GetNext
}

class CounterActor(name: String, initial: Int = 0) extends PersistentActor {
  override def persistenceId = s"counter-$name"

  var count = initial

  def receiveCommand: Actor.Receive = {
    case CounterProtocol.GetNext =>
      val replyTo = sender
      persist(CounterProtocol.GetNext) { _ =>
        count += 1
        replyTo ! count
      }
  }

  def receiveRecover: Actor.Receive = {
    case CounterProtocol.GetNext =>
      count += 1
  }
}
