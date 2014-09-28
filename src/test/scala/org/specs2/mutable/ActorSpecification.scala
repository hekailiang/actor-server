package org.specs2.mutable

import akka.actor._
import akka.cluster.Cluster
import akka.io.Tcp._
import akka.testkit._
import com.secretapp.backend.api.counters._
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

  lazy val actorSystemName = "test-actor-system"

  implicit lazy val system = ActorSystem(actorSystemName)

  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)

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
}

trait ActorSpecification extends ActorLikeSpecification {
  override def is = fragments
}
