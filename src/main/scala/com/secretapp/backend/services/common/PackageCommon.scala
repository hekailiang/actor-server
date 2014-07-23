package com.secretapp.backend.services.common

import PackageCommon._
import akka.actor.ActorRef
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.{MessageBox, Package}
import scalaz._
import scalaz.Scalaz._

object PackageCommon {
  type PackageEither = Package \/ Package

  trait PackageServiceMessage
  case class PackageToSend(p: PackageEither) extends PackageServiceMessage
  case class MessageBoxToSend(mb: MessageBox) extends PackageServiceMessage
  case class UpdateBoxToSend(mb: UpdateBox) extends PackageServiceMessage

  trait ServiceMessage
  case class Authenticate(u: User) extends ServiceMessage
}

trait PackageCommon extends LoggerService with RandomService {
  val handleActor: ActorRef

  def sendReply(reply: PackageEither): Unit = {
    handleActor ! PackageToSend(reply)
  }
}
