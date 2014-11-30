package org.specs2.mutable

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.secretapp.backend.api.counters._
import com.typesafe.config._
import java.net.InetAddress
import org.specs2._
import org.specs2.control._
import org.specs2.execute._
import org.specs2.main.ArgumentsShortcuts
import org.specs2.matcher._
import org.specs2.specification._
import org.specs2.time._

trait ActorLikeSpecification extends SpecificationStructure
with SpecificationStringContext
with mutable.FragmentsBuilder
with mutable.SpecificationInclusion
with ArgumentsArgs
with ArgumentsShortcuts
with MustThrownMatchers
with ShouldThrownMatchers
with StandardResults
with StandardMatchResults
with mutable.Tags
with AutoExamples
with PendingUntilFixed
with Contexts
with SpecificationNavigation
with ContextsInjection
with Debug
with TestKitBase
{
  sequential

  lazy val systemName: String = "secret-api-server"

  lazy val config: Config = createConfig

  implicit lazy val system: ActorSystem = ActorSystem(systemName, config)

  private def shutdownActorSystem() {
    TestKit.shutdownActorSystem(system)
    Thread.sleep(500)
  }

  override def map(fs: => Fragments) = super.map(fs) ^ Step(shutdownActorSystem)

  implicit def anyToSuccess[T]: AsResult[T] = new AsResult[T] {
    def asResult(t: =>T) = {
      t
      success
    }
  }

  private def createConfig: Config = {
    val maxPort = 65535
    val minPort = 1025
    val port = util.Random.nextInt(maxPort - minPort + 1) + minPort

    val host = InetAddress.getLocalHost.getHostAddress

    ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port = $port
        akka.remote.netty.tcp.hostname = "$host"
        akka.cluster.seed-nodes = [ "akka.tcp://$systemName@$host:$port" ]
      """).
      withFallback(ConfigFactory.load().getConfig("actor-server"))
  }
}

trait ActorSpecification extends ActorLikeSpecification {
  override def is = fragments
}
