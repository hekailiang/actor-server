package com.secretapp.backend.sms

import akka.actor._
import scala.util.{ Failure, Success }
import com.typesafe.config._
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.duration._

object SmsEnginesProtocol {
  sealed trait SmsEnginesMessage
  case class Send(phoneNumber: Long, code: String) extends SmsEnginesMessage
  case class ForgetSentCode(phoneNumber: Long, code: String) extends SmsEnginesMessage
}

object SmsEnginesActor {
  def apply()(implicit system: ActorSystem): ActorRef = system.actorOf(
    Props(classOf[SmsEnginesActor]),
    "sms-engines"
  )
}

// TODO: use multiple engines
class SmsEnginesActor() extends Actor with ActorLogging {
  import SmsEnginesProtocol._

  val config = context.system.settings.config.getConfig("sms")

  implicit val ec = context.dispatcher

  private val smsWaitIntervalMs = config.getDuration("sms-wait-interval", TimeUnit.MILLISECONDS)

  private val sentCodes = new mutable.HashSet[(Long, String)]()

  // need separate system because of https://github.com/wandoulabs/spray-websocket/issues/44
  private val spraySystem = ActorSystem("spray-client", ConfigFactory.load().getConfig("spray-client"))

  private val engine = new TwilioSmsEngine(config.getConfig("twilio"))(spraySystem)

  private def codeWasNotSent(phoneNumber: Long, code: String) = !sentCodes.contains((phoneNumber, code))

  private def rememberSentCode(phoneNumber: Long, code: String) = sentCodes += ((phoneNumber, code))

  private def forgetSentCode(phoneNumber: Long, code: String) = sentCodes -= ((phoneNumber, code))

  private def forgetSentCodeAfterDelay(phoneNumber: Long, code: String) =
    context.system.scheduler.scheduleOnce(smsWaitIntervalMs.milliseconds, self, ForgetSentCode(phoneNumber, code))

  private def sendCode(phoneNumber: Long, code: String): Unit = {
    if (codeWasNotSent(phoneNumber, code)) {
      rememberSentCode(phoneNumber, code)
      engine.send(phoneNumber, code) andThen {
        case Success(res) =>
        case Failure(e) =>
          log.error(e, s"Failed to send sms to $phoneNumber with code $code")
      }
      forgetSentCodeAfterDelay(phoneNumber, code)
    } else {
      //log.debug(s"Ignoring send $code to $phoneNumber")
    }
  }

  override def postStop(): Unit = {
    spraySystem.shutdown()
  }

  override def receive: Receive = {
    case Send(phoneNumber, code)           => sendCode(phoneNumber, code)
    case ForgetSentCode(phoneNumber, code) => forgetSentCode(phoneNumber, code)
  }
}
