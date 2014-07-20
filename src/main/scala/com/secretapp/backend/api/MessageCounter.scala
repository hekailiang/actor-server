package com.secretapp.backend.api

import akka.actor._

trait MessageCounter {
  self: Actor with ActorLogging =>

  try {
    val ref = context.actorOf(Props(new CounterActor("message-counter")), "message-counter")
    log.info("Message Counter started on {}", ref.path)
  } catch {
    case e: Throwable =>
      log.warning("message-counter already exists")
  }
}
