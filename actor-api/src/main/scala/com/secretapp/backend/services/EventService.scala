package com.secretapp.backend.services

import com.secretapp.backend.persist.events.LogEvent
import play.api.libs.json._
import scala.concurrent.ExecutionContext

object EventKind extends Enumeration {
  type EventKind = Value
  val AuthCode = Value("auth_code")
  val SignIn = Value("sign_in")
  val SignUp = Value("sign_up")
}

object Event {
  sealed trait EventMessage {
    val klass: Int
    def toJson: String
  }
  case class RpcError(kind: EventKind.EventKind, code: Int, message: String) extends EventMessage {
    val klass = 0
    def toJson = Json.stringify(rpcErrorWrites.writes(this))
  }
  case class SmsSentSuccessfully(body: String, gateResponse: String) extends EventMessage {
    val klass = 1
    def toJson = Json.stringify(smsSentSuccessfullyWrites.writes(this))
  }
  case class SmsFailure(body: String, gateResponse: String) extends EventMessage {
    val klass = 2
    def toJson = Json.stringify(smsFailureWrites.writes(this))
  }
  case class AuthCodeSent(smsHash: String, smsCode: String) extends EventMessage {
    val klass = 3
    def toJson = Json.stringify(authCodeSentWrites.writes(this))
  }
  case class SignedIn(smsHash: String, smsCode: String) extends EventMessage {
    val klass = 4
    def toJson = Json.stringify(signedInWrites.writes(this))
  }
  case class SignedUp(smsHash: String, smsCode: String) extends EventMessage {
    val klass = 5
    def toJson = Json.stringify(signedUpWrites.writes(this))
  }

  implicit val longWrites = new Writes[Long] {
    def writes(n: Long) = JsString(n.toString)
  }
  implicit val rpcErrorWrites = Json.writes[RpcError]
  implicit val smsSentSuccessfullyWrites = Json.writes[SmsSentSuccessfully]
  implicit val smsFailureWrites = Json.writes[SmsFailure]
  implicit val authCodeSentWrites = Json.writes[AuthCodeSent]
  implicit val signedInWrites = Json.writes[SignedIn]
  implicit val signedUpWrites = Json.writes[SignedUp]
}
import Event._

object EventService {
  def log(authId: Long, phoneNumber: Long, message: EventMessage)(implicit ec: ExecutionContext) =
    LogEvent.create(
      authId = authId,
      phoneNumber = phoneNumber,
      email = "",
      klass = message.klass,
      jsonBody = message.toJson
    )
}
