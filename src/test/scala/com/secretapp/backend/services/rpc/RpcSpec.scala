package com.secretapp.backend.services.rpc

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.{ ApiHandlerActor, Counters, CountersProxies }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.{ TransportMessage, RpcResponseBox }
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.CassandraSpecification
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.reflect.ClassTag
import scala.util.Random

trait RpcSpec extends ActorLikeSpecification with CassandraSpecification with ActorServiceHelpers with MockFactory {
  override lazy val actorSystemName = "api"

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs () returning (12345L)
      }
    }
  }

  val smsCode = "test_sms_code"
  val smsHash = "test_sms_hash"
  val userId = 101
  val userSalt = "user_salt"

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
    override def genSmsCode = smsCode
    override def genSmsHash = smsHash
    override def genUserId = userId
    override def genUserAccessSalt = userSalt
  }

  val counters = new Counters
  val countersProxies = new CountersProxies

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, countersProxies)(session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

  implicit class AnyRefWithSpecHelpers(x: AnyRef) {
    def assertInstanceOf[A: ClassTag]: A = {
      x should beAnInstanceOf[A]
      x.asInstanceOf[A]
    }
  }

  implicit class MessageBoxWithSpecHelpers(x: MessageBox) {
    def assertResponseOk[A: ClassTag]: A = {
      x
        .body.assertInstanceOf[RpcResponseBox]
        .body.assertInstanceOf[Ok]
        .body.assertInstanceOf[A]
    }
  }
}
