package com.secretapp.backend.api

import akka.actor.Actor
import com.notnoop.apns.{ APNS, ApnsService }
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.helpers.AuthIdOwnershipHelpers
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scalaz._
import Scalaz._

trait ApplePush extends AuthIdOwnershipHelpers {
  val apnsService: ApnsService

  protected def applePushText(authId: Long, u: updateProto.SeqUpdateMessage)(
    implicit ec: ExecutionContext
  ): Future[Option[String]] = {
    @inline
    def messageText(senderUserId: Int): Future[Option[String]] = {
      getOrSetUserId(authId) map { userId =>
        if (senderUserId == userId)
          None
        else
          "New message".some
      }
    }

    u match {
      case m: updateProto.Message =>
        messageText(m.senderUid)
      case m: updateProto.EncryptedMessage =>
        messageText(m.senderUid)
      case _: updateProto.contact.ContactRegistered =>
        Future.successful("Contact registered".some)
      case _: updateProto.GroupInvite =>
        Future.successful("You are inviter to a group".some)
      case _: updateProto.GroupUserKick =>
        Future.successful("Group user kicked".some)
      case _: updateProto.GroupUserAdded =>
        Future.successful("User added to a group".some)
      case _: updateProto.GroupUserLeave =>
        Future.successful("User left a group".some)
      case _: updateProto.GroupAvatarChanged =>
        Future.successful("Group avatar changed".some)
      case _: updateProto.GroupTitleChanged =>
        Future.successful("Group title changed".some)
      case _ =>
        Future.successful(None)
    }
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
                              (implicit ec: ExecutionContext): Future[Unit] =
    optCreds some { c =>
      sendApplePush(c.token, seq, text)
    } none Future.successful()

  def deliverApplePush(authId: Long, seq: Int, text: Option[String])(implicit ec: ExecutionContext): Future[Unit] =
    persist.ApplePushCredentials.find(authId) flatMap {
      deliverApplePush(_, seq, text)
    }

}
