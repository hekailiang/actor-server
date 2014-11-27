/*package com.secretapp.backend.sms

import java.util.concurrent.TimeUnit

import akka.actor._
import dispatch._, Defaults._
import com.ning.http.client.extra.ThrottleRequestFilter
import com.typesafe.config._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ClickatellSmsEngine extends HttpClient {
  self: ClickatellSmsEngineActor =>

  val httpConfig = config.getConfig("sms.twilio.http")

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

  private def message(code: String) = s"Your secret app activation code: $code"

  def send(phoneNumber: Long, code: String): Future[Unit] = {
    val req = svc
      .addQueryParameter("to", phoneNumber.toString)
      .addQueryParameter("text", message(code))

    http(req).either andThen {
      case Success(v) => v match {
        case Right(_) => //log.debug(s"Sent $code to $phoneNumber")
        case Left(e)  => log.error(e, s"Failed while sending $code to $phoneNumber")
      }
      case Failure(e) => log.error(e, s"Failed while sending $code to $phoneNumber")
    } map { _ => }
  }
}

class ClickatellSmsEngineActor(override val config: Config) extends Actor with ActorLogging with ClickatellSmsEngine {
  import ClickatellSmsEngineActor._

  private val smsWaitIntervalMs = {
    val clickatellConfig = config.getConfig("sms.clickatell")
    clickatellConfig.getDuration("sms-wait-interval", TimeUnit.MILLISECONDS)
  }

  private val sentCodes = new mutable.HashSet[(Long, String)]()

  private def codeWasNotSent(phoneNumber: Long, code: String) = !sentCodes.contains((phoneNumber, code))

  private def rememberSentCode(phoneNumber: Long, code: String) = sentCodes += ((phoneNumber, code))

  private def forgetSentCode(phoneNumber: Long, code: String) = sentCodes -= ((phoneNumber, code))

  private def forgetSentCodeAfterDelay(phoneNumber: Long, code: String) =
    context.system.scheduler.scheduleOnce(smsWaitIntervalMs.milliseconds, self, ForgetSentCode(phoneNumber, code))

  private def sendCode(phoneNumber: Long, code: String): Unit = {
    if (codeWasNotSent(phoneNumber, code)) {
      rememberSentCode(phoneNumber, code)
      send(phoneNumber, code)
      forgetSentCodeAfterDelay(phoneNumber, code)
    } else {
      //log.debug(s"Ignoring send $code to $phoneNumber")
    }
  }

  override def receive: Receive = {
    case Send(phoneNumber, code)           => sendCode(phoneNumber, code)
    case ForgetSentCode(phoneNumber, code) => forgetSentCode(phoneNumber, code)
  }
}

object ClickatellSmsEngineActor {
  case class Send(phoneNumber: Long, code: String)
  case class ForgetSentCode(phoneNumber: Long, code: String)

  def apply(config: Config)(implicit system: ActorSystem): ActorRef = system.actorOf(
    Props(classOf[ClickatellSmsEngineActor], config),
    "clickatell-sms-engine"
  )

  def apply()(implicit system: ActorSystem): ActorRef = ClickatellSmsEngineActor(ConfigFactory.load())
}
 */
