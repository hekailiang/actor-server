package com.secretapp.backend.api.counters

import akka.actor._
import akka.contrib.pattern._

object EmailsCounter {
  def start(implicit system: ActorSystem) = {
    val props = ClusterSingletonManager.props(
      singletonProps = Props(new CounterActor("emails")),
      singletonName = "emails-counter",
      terminationMessage = PoisonPill,
      role = None // TODO: specify roles the singleton should run on
    )
    system.actorOf(props, name = "emails-singleton")
  }

  def startProxy(implicit system: ActorSystem) = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonPath = "/user/emails-singleton/emails-counter",
        role = None),
      name = "emails-counter-proxy")
  }
}
