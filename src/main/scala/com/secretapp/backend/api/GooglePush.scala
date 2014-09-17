package com.secretapp.backend.api

import com.typesafe.config.ConfigFactory
import dispatch._
import dispatch.Defaults._
import scala.concurrent.Future

trait GooglePush {
  self: UpdatesBroker =>

  private val token = ConfigFactory.load().getString("gcm.token")

  private val basicRequest =
    url("https://android.googleapis.com/gcm/send")
      .POST
      .addHeader("Authorization", s"key=$token")
      .setContentType("application/json", "UTF-8")

  private def payload(uid: Int, seq: Int) = s"""{ "uid": $uid, "seq": $seq }"""

  private def request(uid: Int, seq: Int) = basicRequest.setBody(payload(uid, seq))


  def sendGooglePush(uid: Int, seq: Int): Future[Unit] = {
    Http(request(uid, seq) OK as.String).either map {
      case Right() => log.debug("GCM push succeed")
      case Left(e) => log.error(s"GCM push failed: ${e.getMessage}")
    }
  }

}
