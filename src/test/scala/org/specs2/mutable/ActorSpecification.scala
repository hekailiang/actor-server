package org.specs2.mutable

import akka.testkit._
import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString
import scala.collection.immutable.Seq
import org.specs2._
import org.specs2.control._
import org.specs2.execute._
import org.specs2.main.ArgumentsShortcuts
import org.specs2.matcher._
import org.specs2.specification._
import org.specs2.time._
import scodec.bits._
import scalaz._
import Scalaz._

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
with ImplicitSender
{
  sequential

  lazy val actorSystemName = "test-actor-system"

  implicit lazy val system = ActorSystem(actorSystemName)

  private def shutdownActorSystem() {
    TestKit.shutdownActorSystem(system)
  }

  override def map(fs: => Fragments) = super.map(fs) ^ Step(shutdownActorSystem)

  def codecRes2BS(res: String \/ BitVector): ByteString = {
    ByteString(res.toOption.get.toByteBuffer)
  }
}

trait ActorSpecification extends ActorLikeSpecification {
  override def is = fragments
}
