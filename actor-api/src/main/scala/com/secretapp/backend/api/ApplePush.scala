package com.secretapp.backend.api

import akka.actor.Actor
import com.datastax.driver.core.{ Session => CSession }
import com.notnoop.apns.{ APNS, ApnsService }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.{ ExecutionContext, Future }
import scalaz._
import Scalaz._

trait ApplePush {
  val apnsService: ApnsService

  private def payload(seq: Int) =
    APNS.newPayload.forNewsstand().customField("seq", seq).build

  private def sendApplePush(token: String, seq: Int): Future[Unit] = {
    val notification = apnsService.push(token, payload(seq))
    Future.successful()
  }

  private def deliverApplePush(optCreds: Option[models.ApplePushCredentials], seq: Int)
                              (implicit ec: ExecutionContext, s: CSession): Future[Unit] =
    optCreds some { c =>
      sendApplePush(c.token, seq)
    } none Future.successful()

  def deliverApplePush(authId: Long, seq: Int)(implicit ec: ExecutionContext, s: CSession): Future[Unit] =
    persist.ApplePushCredentials.get(authId) flatMap {
      deliverApplePush(_, seq)
    }

}
