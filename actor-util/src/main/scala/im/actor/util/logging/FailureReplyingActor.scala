package im.actor.util.logging

import akka.actor._
import akka.event.LoggingAdapter

trait FailureReplyingActor extends Actor {
  def log: LoggingAdapter

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Unhandled exception for message: {}, replying with Status.Failure", message)
    sender ! Status.Failure(reason)
    super.preRestart(reason, message)
  }
}
