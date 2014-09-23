package com.secretapp.backend.services.common

import PackageCommon._
import akka.actor.ActorRef
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.{MessageBox, MTPackage}
import scalaz._
import scalaz.Scalaz._

object PackageCommon {
  type PackageEither = MTPackage \/ MTPackage

  trait PackageServiceMessage
  case class PackageToSend(connector: ActorRef, p: PackageEither) extends PackageServiceMessage
  case class UpdateBoxToSend(mb: UpdateBox) extends PackageServiceMessage
}
