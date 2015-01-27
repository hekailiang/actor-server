package com.secretapp.backend.models.log

import play.api.libs.json._

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
    val klass = RpcError.klass
    def toJson = Json.stringify(rpcErrorWrites.writes(this))
  }
  case class SmsSentSuccessfully(body: String, gateResponse: String) extends EventMessage {
    val klass = SmsSentSuccessfully.klass
    def toJson = Json.stringify(smsSentSuccessfullyWrites.writes(this))
  }
  case class SmsFailure(body: String, gateResponse: String) extends EventMessage {
    val klass = SmsFailure.klass
    def toJson = Json.stringify(smsFailureWrites.writes(this))
  }
  case class AuthCodeSent(smsHash: String, smsCode: String) extends EventMessage {
    val klass = AuthCodeSent.klass
    def toJson = Json.stringify(authCodeSentWrites.writes(this))
  }
  case class SignedIn(smsHash: String, smsCode: String) extends EventMessage {
    val klass = SignedIn.klass
    def toJson = Json.stringify(signedInWrites.writes(this))
  }
  case class SignedUp(smsHash: String, smsCode: String) extends EventMessage {
    val klass = SignedUp.klass
    def toJson = Json.stringify(signedUpWrites.writes(this))
  }

  object RpcError { val klass = 0 }
  object SmsSentSuccessfully { val klass = 1 }
  object SmsFailure { val klass = 2 }
  object AuthCodeSent { val klass = 3 }
  object SignedIn { val klass = 4 }
  object SignedUp { val klass = 5 }

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
