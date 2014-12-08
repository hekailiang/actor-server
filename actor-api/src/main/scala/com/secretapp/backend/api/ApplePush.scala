package com.secretapp.backend.api

import akka.actor.Actor
import com.datastax.driver.core.{ Session => CSession }
import com.notnoop.apns.{ APNS, ApnsService }
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scalaz._
import Scalaz._

trait ApplePush {
  val apnsService: ApnsService

  protected def applePushText(u: updateProto.SeqUpdateMessage): Option[String] =
    u match {
      case _ @ (_: updateProto.Message | _: updateProto.EncryptedMessage) =>
        "New message".some
      case _: updateProto.contact.ContactRegistered =>
        "Contact registered".some
      case _: updateProto.GroupInvite =>
        "You are inviter to a group".some
      case _: updateProto.GroupUserKick =>
        "Group user kicked".some
      case _: updateProto.GroupUserAdded =>
        "User added to a group".some
      case _: updateProto.GroupUserLeave =>
        "User left a group".some
      case _: updateProto.GroupAvatarChanged =>
        "Group avatar changed".some
      case _: updateProto.GroupTitleChanged =>
        "Group title changed".some
      case _ => None
    }

  private def payload(seq: Int, text: Option[String]) = {
    val builder = APNS.newPayload.forNewsstand().customField("seq", seq)
    text map (builder.alertBody(_)) getOrElse (builder) build
  }

  private def sendApplePush(token: String, seq: Int, text: Option[String]): Future[Unit] = {
    val notification = apnsService.push(token, payload(seq, text))
    Future.successful()
  }

  private def deliverApplePush(optCreds: Option[models.ApplePushCredentials], seq: Int, text: Option[String])
                              (implicit ec: ExecutionContext, s: CSession): Future[Unit] =
    optCreds some { c =>
      sendApplePush(c.token, seq, text)
    } none Future.successful()

  def deliverApplePush(authId: Long, seq: Int, text: Option[String])(implicit ec: ExecutionContext, s: CSession): Future[Unit] =
    persist.ApplePushCredentials.get(authId) flatMap {
      deliverApplePush(_, seq, text)
    }

}
