package com.secretapp.backend.services.rpc

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.Singletons
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import im.actor.server.persist.unit.SqlSpec
import org.specs2.mutable.{ActorReceiveHelpers, ActorLikeSpecification, ActorServiceHelpers}
import scala.util.Random

trait RpcSpec extends ActorLikeSpecification with ActorReceiveHelpers with SqlSpec
    with ActorServiceHelpers with RpcSpecHelpers
    with org.specs2.mutable.SpecificationLike with org.specs2.matcher.ThrownExpectations with org.specs2.time.NoTimeConversions
