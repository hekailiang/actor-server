package com.secretapp.backend.services.rpc

import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.persist.CassandraSpecification
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import akka.actor._
import akka.testkit._
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.ActorLikeSpecification
import org.specs2.mutable.ActorServiceHelpers
import scala.util.Random

trait RpcSpec extends ActorLikeSpecification with CassandraSpecification with ActorServiceHelpers with MockFactory {
  override lazy val actorSystemName = "api"

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs() returning(12345L)
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

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

}
