package com.secretapp.backend.models.log

import spray.json._

object EventKind extends Enumeration {
  type EventKind = Value
  val AuthCode = Value("auth_code")
  val SignIn = Value("sign_in")
  val SignUp = Value("sign_up")
}

object Event extends DefaultJsonProtocol {
  sealed trait EventMessage {
    val klass: Int
    def toJson: String
  }
  case class RpcError(kind: EventKind.EventKind, code: Int, message: String) extends EventMessage {
    val klass = RpcError.klass
    def toJson = rpcErrorWrites.write(this).compactPrint
  }
  case class SmsSentSuccessfully(body: String, gateResponse: String) extends EventMessage {
    val klass = SmsSentSuccessfully.klass
    def toJson = smsSentSuccessfullyWrites.write(this).compactPrint
  }
  case class SmsFailure(body: String, gateResponse: String) extends EventMessage {
    val klass = SmsFailure.klass
    def toJson = smsFailureWrites.write(this).compactPrint
  }
  case class AuthCodeSent(smsHash: String, smsCode: String) extends EventMessage {
    val klass = AuthCodeSent.klass
    def toJson = authCodeSentWrites.write(this).compactPrint
  }
  case class SignedIn(smsHash: String, smsCode: String) extends EventMessage {
    val klass = SignedIn.klass
    def toJson = signedInWrites.write(this).compactPrint
  }
  case class SignedUp(smsHash: String, smsCode: String) extends EventMessage {
    val klass = SignedUp.klass
    def toJson = signedUpWrites.write(this).compactPrint
  }

  implicit object LongWrites extends RootJsonFormat[Long] {
    def write(n: Long) = JsString(n.toString)
    def read(v: JsValue) = ???
  }
  implicit object EventKindWrites extends RootJsonFormat[EventKind.EventKind] {
    def write(k: EventKind.EventKind) = JsString(k.toString)
    def read(v: JsValue) = ???
  }
  implicit val rpcErrorWrites = jsonFormat3(RpcError.apply)
  implicit val smsSentSuccessfullyWrites = jsonFormat2(SmsSentSuccessfully.apply)
  implicit val smsFailureWrites = jsonFormat2(SmsFailure.apply)
  implicit val authCodeSentWrites = jsonFormat2(AuthCodeSent.apply)
  implicit val signedInWrites = jsonFormat2(SignedIn.apply)
  implicit val signedUpWrites = jsonFormat2(SignedUp.apply)

  object RpcError { val klass = 0 }
  object SmsSentSuccessfully { val klass = 1 }
  object SmsFailure { val klass = 2 }
  object AuthCodeSent { val klass = 3 }
  object SignedIn { val klass = 4 }
  object SignedUp { val klass = 5 }
}
