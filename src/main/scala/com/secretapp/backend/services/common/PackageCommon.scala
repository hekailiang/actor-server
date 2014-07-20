package com.secretapp.backend.services.common

import akka.actor.ActorRef
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.Package
import scalaz._
import Scalaz._

object PackageCommon {
  type PackageEither = Package \/ Package

  trait PackageServiceMessage
  case class PackageToSend(p: PackageEither) extends PackageServiceMessage
  case class MessageBoxToSend(mb: MessageBox) extends PackageServiceMessage

  trait ServiceMessage
  case class Authenticate(userId: Long, u: User) extends ServiceMessage
}
import PackageCommon._

trait PackageCommon extends LoggerService with RandomService {
  val handleActor: ActorRef

  def sendReply(reply: PackageEither): Unit = {
    handleActor ! PackageToSend(reply)
  }
}
