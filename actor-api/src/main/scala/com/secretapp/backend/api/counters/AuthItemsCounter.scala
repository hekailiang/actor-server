package com.secretapp.backend.api.counters

import akka.actor._
import akka.contrib.pattern._

object AuthItemsCounter {
  def start(implicit system: ActorSystem) = {
    val props = ClusterSingletonManager.props(
      singletonProps = Props(new CounterActor("auth-items")),
      singletonName = "auth-items-counter",
      terminationMessage = PoisonPill,
      role = None // TODO: specify roles the singleton should run on
    )
    system.actorOf(props, name = "auth-items-singleton")
  }

  def startProxy(implicit system: ActorSystem) = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonPath = "/user/auth-items-singleton/auth-items-counter",
        role = None),
      name = "auth-items-counter-proxy")
  }
}
