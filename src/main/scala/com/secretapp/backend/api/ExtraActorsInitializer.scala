package com.secretapp.backend.api

import akka.actor._

trait ExtraActorsInitializer {
  self: Actor with ActorLogging =>
    context.system.actorOf(Props(new CounterActor("user-counter")), "user-counter")
}
