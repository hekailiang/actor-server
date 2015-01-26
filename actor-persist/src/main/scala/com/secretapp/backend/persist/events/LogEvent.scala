package com.secretapp.backend.persist.events

import com.secretapp.backend.persist.Paginator
import scala.concurrent._
import scalikejdbc._
import org.joda.time.DateTime

case class LogEvent(id: Long, authId: Long, phoneNumber: Long, email: String, klass: Int,
                    jsonBody: String, createdAt: DateTime)

object LogEvent extends SQLSyntaxSupport[LogEvent] with Paginator[LogEvent] {
  override val tableName = "log_events"
  override val columnNames = Seq("id", "auth_id", "phone_number", "email", "klass", "json_body", "created_at")

  val publicColumns = columnNames
  override val digitColumns = Set("id", "auth_id", "phone_number", "klass")
  lazy val alias = LogEvent.syntax("le")

  def apply(a: SyntaxProvider[LogEvent])(rs: WrappedResultSet): LogEvent = apply(alias.resultName)(rs)

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

  def all(req: Map[String, Seq[String]] = Map())
         (implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[(Seq[LogEvent], Int)] =
    Future {
      blocking {
        paginate(req)
      }
    }
}
