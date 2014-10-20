package com.secretapp.backend

import akka.actor.ActorSystem
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.services.rpc.presence.{ GroupPresenceBroker, PresenceBroker }
import com.secretapp.backend.services.rpc.typing.TypingBroker
import com.secretapp.backend.sms.ClickatellSmsEngineActor
import com.datastax.driver.core.{ Session => CSession }
import com.notnoop.apns.APNS
import com.typesafe.config.ConfigFactory

package object api {
  final class Singletons(implicit val system: ActorSystem, csession: CSession) {
    val config = ConfigFactory.load()
    val appConfig = config.getConfig("secret")
    val filesCounter = FilesCounter.start(system)
    val smsEngine = ClickatellSmsEngineActor()
    val typingBrokerRegion = TypingBroker.startRegion()
    val presenceBrokerRegion = PresenceBroker.startRegion()
    val groupPresenceBrokerRegion = GroupPresenceBroker.startRegion()
    val apnsService = APNS.newService.withCert(
      appConfig.getString("apns.cert.path"),
      appConfig.getString("apns.cert.password")
    ).withProductionDestination.build
  }

  final class ClusterProxies(implicit val system: ActorSystem) {
    val filesCounter = FilesCounter.startProxy(system)
  }
}
