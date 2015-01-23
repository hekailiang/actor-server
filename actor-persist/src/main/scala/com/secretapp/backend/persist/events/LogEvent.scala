package com.secretapp.backend.persist.events

import scala.concurrent._
import scalikejdbc._

object LogEvent {
  object EventKind extends Enumeration {
    type EventKind = Value
    val AuthCode, SignIn, SignUp = Value
  }
  sealed trait EventMessage {
    val klass: Int
  }
  case class RpcError(kind: EventKind.EventKind, code: Int, message: String) extends EventMessage { val klass = 0 }
  case class SmsSentSuccessfully(body: String, gateResponse: String) extends EventMessage { val klass = 1 }
  case class SmsFailure(body: String, gateResponse: String) extends EventMessage { val klass = 2 }
  case class AuthCodeSent(smsHash: String, smsCode: String) extends EventMessage { val klass = 3 }
  case class SignedIn(smsHash: String, smsCode: String) extends EventMessage { val klass = 4 }
  case class SignedUp(smsHash: String, smsCode: String) extends EventMessage { val klass = 5 }

  def log(authId: Long, phoneNumber: Long, message: EventMessage) =
    Future.successful(())
}
