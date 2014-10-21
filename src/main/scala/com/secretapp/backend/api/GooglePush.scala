package com.secretapp.backend.api

import akka.actor.ActorLogging
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.models.GooglePushCredentials
import com.secretapp.backend.persist.GooglePushCredentialsRecord
import com.typesafe.config.ConfigFactory
import dispatch._
import dispatch.Defaults._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait GooglePush {
  self: ActorLogging =>

  private val token = ConfigFactory.load().getString("gcm.token")
  private val dryRun = ConfigFactory.load().getBoolean("gcm.dry-run")

  private val basicRequest =
    url("https://android.googleapis.com/gcm/send")
      .POST
      .addHeader("Authorization", s"key=$token")
      .setContentType("application/json", "UTF-8")

  private def payload(regId: String, seq: Int) =
    s"""
       |{
       |  "registration_ids": ["$regId"],
       |  "dry_run": $dryRun,
       |  "data": {
       |    "seq": $seq
       |  }
       |}""".stripMargin

  private def request(regId: String, seq: Int) = basicRequest.setBody(payload(regId, seq))


  private def sendGooglePush(regId: String, seq: Int): Future[Unit] =
    Http(request(regId, seq) OK as.String).either map {
      case Right(_) => log.debug("GCM push succeed")
      case Left(e)  => log.error(s"GCM push failed: ${e.getMessage}")
    }

  private def deliverGooglePush(optCreds: Option[GooglePushCredentials], seq: Int)
                               (implicit s: CSession): Future[Unit] =
    optCreds some { c =>
      sendGooglePush(c.regId, seq)
    } none Future.successful()

  def deliverGooglePush(authId: Long, seq: Int)
                       (implicit s: CSession): Future[Unit] =
    GooglePushCredentialsRecord.get(authId) flatMap {
      deliverGooglePush(_, seq)
    }

}
