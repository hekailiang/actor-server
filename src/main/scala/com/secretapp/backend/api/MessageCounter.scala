package com.secretapp.backend.api

import akka.actor._

trait MessageCounter {
  self: Actor with ActorLogging =>

  val messageCounter = context.actorOf(Props(new CounterActor("message-counter")), "message-counter")
}
