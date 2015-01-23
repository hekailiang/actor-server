package com.secretapp.backend.persist.events

import scala.concurrent._
import scalikejdbc._
import org.joda.time.DateTime

object EventKind extends Enumeration {
  type EventKind = Value
  val AuthCode, SignIn, SignUp = Value
}

object Event {
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

private case class LogEvent(id: Long, authId: Long, phoneNumber: Long, email: String, klass: Int,
                            jsonBody: String, createdAt: DateTime)

private object LogEvent extends SQLSyntaxSupport[LogEvent] {
  override val tableName = "log_events"
  override val columnNames = Seq("id", "auth_id", "phone_number", "email", "klass", "json_body", "created_at")

  lazy val le = LogEvent.syntax("le")

  def apply(a: SyntaxProvider[LogEvent])(rs: WrappedResultSet): LogEvent = apply(le.resultName)(rs)

  def apply(e: ResultName[LogEvent])(rs: WrappedResultSet): LogEvent =
    LogEvent(
      id = rs.long(e.id),
      authId = rs.long(e.authId),
      phoneNumber = rs.long(e.phoneNumber),
      email = rs.string(e.email),
      klass = rs.int(e.klass),
      jsonBody = rs.string(e.jsonBody),
      createdAt = rs.get[DateTime](e.createdAt)
    )

  def create(authId: Long, phoneNumber: Long, email: String, klass: Int, jsonBody: String)
            (implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Unit] =
    Future {
      blocking {
        withSQL {
          insert.into(LogEvent).namedValues(
            column.authId -> authId,
            column.phoneNumber -> phoneNumber,
            column.email -> email,
            column.klass -> klass,
            column.jsonBody -> jsonBody
          )
        }.execute().apply
      }
    }
}
