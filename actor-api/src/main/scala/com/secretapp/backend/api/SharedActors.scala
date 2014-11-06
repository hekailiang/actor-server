package com.secretapp.backend.api

import akka.actor._
import akka.event.Logging
import akka.util.Timeout
import scala.concurrent.Future
import scala.util.Failure

object SharedActors {
  def log(implicit system: ActorSystem) = Logging.getLogger(system, this)

  /**
    * Resolves ActorRef by path and creates a new actor if is is absent
    */
  def lookup(path: String)(f: => ActorRef)(implicit system: ActorSystem, timeout: Timeout): Future[ActorRef] = {
    val selection = system.actorSelection(path)
    import system.dispatcher

    selection.resolveOne recover {
      case e: ActorNotFound =>
        log.info(s"Creating $path")
        val ref = f
        log.info(s"Created $path")
        ref
    } andThen {
      case Failure(e) =>
        log.error(s"Cannot resolve $path {}", e)
    }
  }
}
