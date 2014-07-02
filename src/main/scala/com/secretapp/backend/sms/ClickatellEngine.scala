package com.secretapp.backend.sms

import dispatch._, Defaults._
import com.ning.http.client.extra.ThrottleRequestFilter
import scala.collection.immutable
import com.typesafe.config._

sealed trait HttpDispatch {
  this: ClickatellSMSEngine =>

  val httpConfig = config.getConfig("sms.clickatell.http")
  val connectionTimeoutMs = httpConfig.getInt("connection-timeout-ms")
  val poolingConnection = httpConfig.getBoolean("pooling-connection")
  val maximumConnectionsTotal = httpConfig.getInt("maximum-connections-total")
  val throttleRequest = httpConfig.getInt("throttle-request")

  lazy val http = new Http()
    .configure(builder => {
      builder.setConnectionTimeoutInMs(connectionTimeoutMs)
      builder.setAllowPoolingConnection(poolingConnection)
      builder.setMaximumConnectionsTotal(maximumConnectionsTotal)
      builder.addRequestFilter(new ThrottleRequestFilter(throttleRequest));
      builder.build()
      builder
    })
}

class ClickatellSMSEngine(val config: Config) extends SMSEngine with HttpDispatch {
  def this() = this(ConfigFactory.load())

  config.checkValid(ConfigFactory.defaultReference(), "sms.clickatell")
  val user = config.getString("sms.clickatell.user")
  val password = config.getString("sms.clickatell.password")
  val apiId = config.getString("sms.clickatell.api-id")

  lazy val svc = url(s"http://api.clickatell.com/http/sendmsg")
    .addHeader("Connection", "Keep-Alive")

  def send(number: String, text: String): Future[String] = {
    val req = svc <<? immutable.Map("to" -> number, "text" -> text,"user" -> user, "password" -> password, "api_id" -> apiId)
    http(req OK as.String)
  }
}
