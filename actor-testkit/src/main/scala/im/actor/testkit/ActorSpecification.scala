package im.actor.testkit

import akka.actor._
import akka.testkit._
import org.specs2.execute.Success
import org.specs2.mutable.BeforeAfter
import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.specification.{ Step, Fragments }

abstract class ActorSpecification(system: ActorSystem) extends TestKit(system)
    with SpecificationLike
    with NoTimeConversions
    with ImplicitSender {
  sequential

  implicit def anyToSuccess[T](a: T): org.specs2.execute.Result = Success()

  protected def beforeAll = {}

  protected def afterAll = {
    system.shutdown()
  }

  override def map(fs: => Fragments) = Step(beforeAll) ^ fs ^ Step(afterAll)
}
