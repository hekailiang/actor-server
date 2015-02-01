package com.secretapp.backend.persist.events

import com.secretapp.backend.models.log.Event
import com.secretapp.backend.persist.Paginator
import scala.concurrent._
import scalikejdbc._
import org.joda.time.DateTime

case class LogEvent(id: Long, authId: Long, phoneNumber: Long, email: String, klass: Int,
                    jsonBody: String, createdAt: DateTime)

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

  def authCodesStat()(implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Seq[(String, Int)]] =
    Future {
      blocking {
        val q = sql"""
                   select (created_at::date) as day, count(*) as count
                   from $table
                   where klass = ${Event.AuthCodeSent.klass}
                   group by day
                   order by day asc
                   """
        q.map { rs =>
          (rs.date("day").toString, rs.int("count"))
        }.list().apply()
      }
    }

  def sentSmsStat()(implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Seq[(String, Int)]] =
    Future {
      blocking {
        val q = sql"""
                   select (created_at::date) as day, count(*) as count
                   from $table
                   where klass IN (${Event.SmsSentSuccessfully.klass}, ${Event.SmsFailure.klass})
                   group by day
                   order by day asc
                   """
        q.map { rs =>
          (rs.date("day").toString, rs.int("count"))
        }.list().apply()
      }
    }

  def successSignsStat()(implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Seq[(String, Int)]] =
    Future {
      blocking {
        val q = sql"""
                   select (created_at::date) as day, count(*) as count
                   from $table
                   where klass IN (${Event.SignedIn.klass}, ${Event.SignedUp.klass})
                   group by day
                   order by day asc
                   """
        q.map { rs =>
          (rs.date("day").toString, rs.int("count"))
        }.list().apply()
      }
    }


  /**
   * @return a seq with format: (Date, Sign in count, Sign up count)
   */
  def authsStat()(implicit ec: ExecutionContext, session: DBSession = LogEvent.autoSession): Future[Seq[(String, Int, Int)]] =
    Future {
      blocking {
        val q = sql"""
                   select day, sum(sign_up_count) as sign_up_count, sum(sign_in_count) as sign_in_count from (
                      select (created_at::date) as day, 0 as sign_up_count, count(*) as sign_in_count
                      from $table
                      where klass = ${Event.SignedIn.klass}
                      group by day

                      union all

                      select (created_at::date) as day, count(*) as sign_up_count, 0 as sign_in_count
                      from $table
                      where klass = ${Event.SignedUp.klass}
                      group by day
                   ) as stats
                   group by day
                   order by day asc
                   """
        q.map { rs =>
          (rs.date("day").toString, rs.int("sign_in_count"), rs.int("sign_up_count"))
        }.list().apply()
      }
    }

}
