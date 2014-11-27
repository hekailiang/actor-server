package com.secretapp.backend.sms

import akka.actor._
import com.typesafe.config._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Failure, Success }

import spray.http._
import spray.client.pipelining._

class TwilioSmsEngine(val config: Config)(implicit system: ActorSystem) extends SmsEngine {
  implicit val ec = system.dispatcher

  private val account = config.getString("sms.twilio.account")
  private val token   = config.getString("sms.twilio.token")
  private val from = config.getString("sms.twilio.from")

  private val pipeline: HttpRequest => Future[HttpResponse] = (
    addCredentials(BasicHttpCredentials(account, token))
      ~> sendReceive
  )

  def send(phoneNumber: Long, code: String): Future[Unit] = {
    val to = "+" + phoneNumber.toString
    val body = message(code)

    pipeline(
      Post(
        s"https://api.twilio.com/2010-04-01/Accounts/$account/Messages.json",
        FormData(
          Map(
            "From" -> from,
            "To" -> to,
            "Body" -> body
          )
        )
      )
    ) map { resp =>
      if (resp.status.intValue != 201)
        throw new Exception(s"Wrong response $resp")
      else ()
    }
  }
}
