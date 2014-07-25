package com.secretapp.backend.api

import akka.actor._
import akka.persistence._

sealed case class Cmd(data: String)
sealed case class Evt(data: String)

object CounterProtocol {
  sealed trait Request

  case object GetNext extends Request
  case class GetBulk(size: Int) extends Request

  sealed trait Response
  case class Bulk(first: Int, last: Int) extends Response
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
    case CounterProtocol.GetBulk(size) =>
      val replyTo = sender
      persist(CounterProtocol.GetBulk(size)) { _ =>
        val first = count + 1
        val last = count + size
        count = last
        replyTo ! CounterProtocol.Bulk(first, last)
      }
  }

  def receiveRecover: Actor.Receive = {
    case CounterProtocol.GetNext =>
      count += 1
    case CounterProtocol.GetBulk(size) =>
      count += size
  }
}
