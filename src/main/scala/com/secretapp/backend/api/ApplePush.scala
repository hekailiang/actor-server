package com.secretapp.backend.api

import akka.actor.ActorLogging
import com.datastax.driver.core.{ Session => CSession }
import com.notnoop.apns.{ APNS, ApnsService }
import com.secretapp.backend.data.models.ApplePushCredentials
import com.secretapp.backend.persist.ApplePushCredentialsRecord
import com.typesafe.config.ConfigFactory
import dispatch._
import dispatch.Defaults._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait ApplePush {
  self: ActorLogging =>

  val apnsService: ApnsService

  private def payload(seq: Int) = {
    APNS.newPayload.forNewsstand().sound("").customField("seq", seq).build
  }

  private def sendApplePush(token: String, seq: Int): Future[Unit] = {
    val notification = apnsService.push(token, payload(seq))
    log.debug(s"Apple notification $notification")
    Future.successful()
  }

  private def deliverApplePush(uid: Int, authId: Long, seq: Int, optCreds: Option[ApplePushCredentials])
                               (implicit s: CSession): Future[Unit] =
    optCreds some { c =>
      log.debug(s"Sending apple push creds=$c, seq=$seq")
      sendApplePush(c.token, seq)
    } none Future.successful()

  def deliverApplePush(uid: Int, authId: Long, seq: Int)
                       (implicit s: CSession): Future[Unit] =
    ApplePushCredentialsRecord.get(uid, authId) flatMap {
      deliverApplePush(uid, authId, seq, _)
    }

}
