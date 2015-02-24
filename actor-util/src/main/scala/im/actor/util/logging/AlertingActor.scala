package im.actor.util.logging

import akka.actor._
import akka.event.LoggingAdapter

trait AlertingActor extends Actor {
  def log: LoggingAdapter

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Unhandled exception for message: {}", message)
    super.preRestart(reason, message)
  }
}
