package com.secretapp.backend.services.common

import PackageCommon._
import akka.actor.ActorRef
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.{JsonPackage, TransportPackage, MessageBox, MTPackage}
import scalaz._
import scalaz.Scalaz._

object PackageCommon {
  trait PackageServiceMessage
  case class UpdateBoxToSend(mb: UpdateBox) extends PackageServiceMessage
}
