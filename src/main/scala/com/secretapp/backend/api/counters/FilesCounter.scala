package com.secretapp.backend.api.counters

import akka.actor._
import akka.contrib.pattern._

object FilesCounter {
  def start(implicit system: ActorSystem) = {
    println("starting files singleton")
    val props = ClusterSingletonManager.props(
      singletonProps = Props(new CounterActor("files")),
      singletonName = "files-counter",
      terminationMessage = PoisonPill,
      role = None // TODO: specify roles the singleton should run on
    )
    system.actorOf(props, name = "singleton")
  }
}
