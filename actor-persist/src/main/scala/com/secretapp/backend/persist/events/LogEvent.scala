package com.secretapp.backend.persist.events

import com.secretapp.backend.models.log.Event
import com.secretapp.backend.persist.Paginator
import scala.concurrent._
import scalikejdbc._
import org.joda.time.DateTime

case class LogEvent(id: Long, authId: Long, phoneNumber: Long, email: String, klass: Int,
                    jsonBody: String, createdAt: DateTime)

case class LogEventStatItem(date: String, requestAuthCount: Int, sentSmsCount: Int, signInCount: Int, signUpCount: Int)

object LogEvent extends SQLSyntaxSupport[LogEvent] with Paginator[LogEvent] {
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

  def all(req: Map[String, Seq[String]])
         (implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[(Seq[LogEvent], Int)] =
    Future {
      blocking {
        paginateWithTotal(select.from(this as le).toSQLSyntax, le, req, Some(("id", DESC)))(this(le))
      }
    }

  def stats()(implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Seq[LogEventStatItem]] =
    Future {
      blocking {
        val klassSeq = Seq(Event.AuthCodeSent.klass, Event.SmsSentSuccessfully.klass, Event.SignedIn.klass, Event.SignedUp.klass)
        val q = sql"""
                   select (created_at::date) as day, klass, count(klass) as klass_count
                   from $table
                   where klass in ($klassSeq)
                   group by day, klass
                   order by day asc
                   """
        val entries = q.map { rs => (rs.string("day"), rs.int("klass"), rs.int("klass_count")) }.list().apply()

        @inline
        def applyValue(entry: LogEventStatItem, klass: Int, klassCount: Int): LogEventStatItem = klass match {
          case Event.AuthCodeSent.klass => entry.copy(requestAuthCount = klassCount)
          case Event.SmsSentSuccessfully.klass => entry.copy(sentSmsCount = klassCount)
          case Event.SignedIn.klass => entry.copy(signInCount = klassCount)
          case Event.SignedUp.klass => entry.copy(signUpCount = klassCount)
        }

        entries.foldLeft(List[LogEventStatItem]()) { (acc, entry) =>
          acc match {
            case x :: xs if x.date == entry._1 => xs.+:(applyValue(x, entry._2, entry._3))
            case _ =>
              val item = LogEventStatItem(
                date = entry._1,
                requestAuthCount = 0,
                sentSmsCount = 0,
                signInCount = 0,
                signUpCount = 0)
              acc.+:(applyValue(item, entry._2, entry._3))
          }
        }.reverse
      }
    }
}
