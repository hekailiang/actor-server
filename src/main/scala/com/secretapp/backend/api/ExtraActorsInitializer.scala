package com.secretapp.backend.api

import akka.actor._

trait ExtraActorsInitializer {
  self: Actor with ActorLogging =>
    val ref = context.system.actorOf(Props(new CounterActor("message-counter")), "message-counter")
    log.info("MessageCounter started {}", ref.path.toStringWithoutAddress)
    context.system.actorOf(Props(new CounterActor("user-counter")), "user-counter")
}
