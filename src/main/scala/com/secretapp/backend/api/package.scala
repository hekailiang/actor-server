package com.secretapp.backend

import akka.actor.ActorSystem
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.services.rpc.presence.PresenceBroker

package object api {
  final class Singletons(implicit val system: ActorSystem) {
    val filesCounter = FilesCounter.start(system)
    val presenceBroker = PresenceBroker.start(system)
  }

  final class ClusterProxies(implicit val system: ActorSystem) {
    val filesCounter = FilesCounter.startProxy(system)
    val presenceBroker = PresenceBroker.startProxy(system)
  }
}
