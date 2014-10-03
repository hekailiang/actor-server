package com.secretapp.backend.services.rpc

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.{ Singletons, ClusterProxies }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.CassandraSpecification
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ActorReceiveHelpers, ActorLikeSpecification, ActorServiceHelpers}
import scala.util.Random

trait RpcSpec extends ActorLikeSpecification with ActorReceiveHelpers with CassandraSpecification
  with ActorServiceHelpers with MockFactory with RpcSpecHelpers {
  override lazy val actorSystemName = "api"
}
