package com.secretapp.backend

import akka.actor.{ ActorSystem, Props }
import com.secretapp.backend.api._

trait ExtraActorsInitializer {
  def initExtraActors(system: ActorSystem): Unit = {
    system.actorOf(Props(new CounterActor("message-counter")), "message-counter")
    system.actorOf(Props(new CounterActor("user-counter")), "user-counter")
  }
}
