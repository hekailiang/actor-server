package com.secretapp.backend

import akka.actor.ActorSystem
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.services.rpc.presence.PresenceBroker
import com.secretapp.backend.services.rpc.typing.TypingBroker
import com.secretapp.backend.sms.ClickatellSmsEngineActor

package object api {
  final class Singletons(implicit val system: ActorSystem) {
    val filesCounter = FilesCounter.start(system)
    val smsEngine = ClickatellSmsEngineActor()
    val typingBrokerRegion = TypingBroker.startRegion()
    val presenceBrokerRegion = PresenceBroker.startRegion()
  }

  final class ClusterProxies(implicit val system: ActorSystem) {
    val filesCounter = FilesCounter.startProxy(system)
  }
}
