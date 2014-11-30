package com.secretapp.backend.api

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.typesafe.config.ConfigFactory
import dispatch._
import dispatch.Defaults._
import im.actor.util.logging.MDCActorLogging
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait GooglePush {
  self: Actor with MDCActorLogging =>

  private val token = context.system.settings.config.getString("gcm.token")
  private val dryRun = context.system.settings.config.getBoolean("gcm.dry-run")

  private val basicRequest =
    url("https://android.googleapis.com/gcm/send")
      .POST
      .addHeader("Authorization", s"key=$token")
      .setContentType("application/json", "UTF-8")

  private def payload(regId: String, collapseKey: String, seq: Int) =
    s"""
       |{
       |  "registration_ids": ["$regId"],
       |  "collapse_key": "$collapseKey",
       |  "dry_run": $dryRun,
       |  "data": {
       |    "seq": $seq
       |  }
       |}""".stripMargin

  private def request(regId: String, collapseKey: String, seq: Int) = basicRequest.setBody(payload(regId, collapseKey, seq))


  private def sendGooglePush(regId: String, collapseKey: String, seq: Int): Future[Unit] =
    Http(request(regId, collapseKey, seq) OK as.String).either map {
      case Right(_) =>
      case Left(e)  => log.error(s"GCM push failed: ${e.getMessage}")
    }

  private def deliverGooglePush(optCreds: Option[models.GooglePushCredentials], authId: Long, seq: Int)
                               (implicit s: CSession): Future[Unit] =
    optCreds some { c =>
      sendGooglePush(c.regId, authId.toString, seq)
    } none Future.successful()

  def deliverGooglePush(authId: Long, seq: Int)
                       (implicit s: CSession): Future[Unit] =
    persist.GooglePushCredentials.get(authId) flatMap {
      deliverGooglePush(_, authId, seq)
    }

}
