package com.secretapp.backend.sms

import scala.util.{ Failure, Success }

import akka.actor._
import com.ning.http.client.extra.ThrottleRequestFilter
import com.typesafe.config._
import dispatch.Defaults._
import dispatch._

class ClickatellSmsEngine(val config: Config)(implicit system: ActorSystem) extends SmsEngine {
  private val http = {
    val httpConfig = config.getConfig("http")
    val connectionTimeoutMs = httpConfig.getInt("connection-timeout-ms")
    val poolingConnection = httpConfig.getBoolean("pooling-connection")
    val maximumConnectionsTotal = httpConfig.getInt("maximum-connections-total")
    val throttleRequest = httpConfig.getInt("throttle-request")

    new Http().configure { builder =>
      builder.setConnectionTimeoutInMs(connectionTimeoutMs)
      builder.setAllowPoolingConnection(poolingConnection)
      builder.setMaximumConnectionsTotal(maximumConnectionsTotal)
      builder.addRequestFilter(new ThrottleRequestFilter(throttleRequest))
      builder.build()
      builder
    }
  }

  private val svc = {
    val user = config.getString("user")
    val password = config.getString("password")
    val apiId = config.getString("api-id")

    url(s"http://api.clickatell.com/http/sendmsg")
      .addHeader("Connection", "Keep-Alive")
      .addQueryParameter("user", user)
      .addQueryParameter("password", password)
      .addQueryParameter("api_id", apiId)
  }

  override def send(phoneNumber: Long, code: String): Future[String] = {
    val req = svc
      .addQueryParameter("to", phoneNumber.toString)
      .addQueryParameter("text", message(code))

    http(req).either andThen {
      case Success(v) => v match {
        case Right(_) => system.log.debug("Sent {} to {}", code, phoneNumber)
        case Left(e) => system.log.error(e, "Failed while sending {} to {}", code, phoneNumber)
      }
      case Failure(e) => system.log.error(e, "Failed while sending {} to {}", code, phoneNumber)
    } map { _ => "OK" }
  }
}
