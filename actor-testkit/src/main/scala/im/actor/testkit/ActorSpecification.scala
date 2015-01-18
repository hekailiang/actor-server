package im.actor.testkit

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import java.net.InetAddress
import org.specs2.execute.Success
import org.specs2.mutable.BeforeAfter
import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.specification.{ Step, Fragments }

object ActorSpecification {
  def createSystem(systemName: String = "actor-server-test"): ActorSystem = {
    ActorSystem(systemName, createConfig(systemName))
  }

  def createConfig(systemName: String = "actor-server-test"): Config = {
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

abstract class ActorSpecification(system: ActorSystem = ActorSpecification.createSystem()) extends TestKit(system)
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
