package com.secretapp.backend.api.counters

import akka.actor._
import akka.contrib.pattern._

object UsersCounter {
  def start(implicit system: ActorSystem) = {
    val props = ClusterSingletonManager.props(
      singletonProps = Props(new CounterActor("users")),
      singletonName = "users-counter",
      terminationMessage = PoisonPill,
      role = None // TODO: specify roles the singleton should run on
    )
    system.actorOf(props, name = "users-singleton")
  }

  def startProxy(implicit system: ActorSystem) = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonPath = "/user/users-singleton/users-counter",
        role = None),
      name = "users-counter-proxy")
  }
}
