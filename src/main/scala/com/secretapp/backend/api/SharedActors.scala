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
    val spath = s"shared/$path"
    val selection = system.actorSelection(spath)
    import system.dispatcher

    selection.resolveOne recover {
      case e: ActorNotFound =>
        log.info(s"Creating $spath")
        val ref = f
        log.info(s"Created $spath")
        ref
    } andThen {
      case Failure(e) =>
        log.error(s"Cannot resolve $spath", e)
    }
  }
}
