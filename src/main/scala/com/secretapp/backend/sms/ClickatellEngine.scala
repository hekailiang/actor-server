package com.secretapp.backend.sms

import dispatch._, Defaults._
import com.ning.http.client.extra.ThrottleRequestFilter
import scala.collection.immutable
import com.typesafe.config._

class ClickatellSMSEngine(val config: Config) extends SMSEngine {

  private val http = {
    val httpConfig              = config.getConfig("sms.clickatell.http")
    val connectionTimeoutMs     = httpConfig.getInt("connection-timeout-ms")
    val poolingConnection       = httpConfig.getBoolean("pooling-connection")
    val maximumConnectionsTotal = httpConfig.getInt("maximum-connections-total")
    val throttleRequest         = httpConfig.getInt("throttle-request")

    new Http()
      .configure(builder => {
        builder.setConnectionTimeoutInMs(connectionTimeoutMs)
        builder.setAllowPoolingConnection(poolingConnection)
        builder.setMaximumConnectionsTotal(maximumConnectionsTotal)
        builder.addRequestFilter(new ThrottleRequestFilter(throttleRequest))
        builder.build()
        builder
      })
  }

  private val svc = {
    config.checkValid(ConfigFactory.defaultReference(), "sms.clickatell")
    val user     = config.getString("sms.clickatell.user")
    val password = config.getString("sms.clickatell.password")
    val apiId    = config.getString("sms.clickatell.api-id")

    url(s"http://api.clickatell.com/http/sendmsg")
      .addHeader("Connection", "Keep-Alive")
      .addQueryParameter("user", user)
      .addQueryParameter("password", password)
      .addQueryParameter("api_id", apiId)
  }

  override def send(phoneNumber: Long, text: String): Future[String] = {
    val req = svc
      .addQueryParameter("to", phoneNumber.toString)
      .addQueryParameter("text", text)

    http(req OK as.String)
  }
}

object ClickatellSMSEngine {
  def apply(): ClickatellSMSEngine = new ClickatellSMSEngine(ConfigFactory.load())
}
