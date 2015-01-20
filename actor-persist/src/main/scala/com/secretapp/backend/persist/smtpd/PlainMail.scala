package com.secretapp.backend.persist.smtpd

import akka.util.ByteString
import scala.concurrent.{ExecutionContext, Future}
import scalikejdbc._

case class PlainMail(randomId: Long, mailFrom: String, recipients: Set[String], message: ByteString)

object PlainMail extends SQLSyntaxSupport[PlainMail] {
  override val tableName = "plain_mails"
  override val columnNames = Seq("id", "random_id", "mail_from", "recipients", "message")

  lazy val pm = PlainMail.syntax("pm")

  def apply(a: SyntaxProvider[PlainMail])(rs: WrappedResultSet): PlainMail = apply(pm.resultName)(rs)

  def apply(m: ResultName[PlainMail])(rs: WrappedResultSet): PlainMail =
    PlainMail(
      randomId = rs.long(m.randomId),
      mailFrom = rs.string(m.mailFrom),
      recipients = rs.string(m.recipients).split(",").toSet,
      message = ByteString(rs.string(m.message))
    )

  def create(randomId: Long, mailFrom: String, recipients: Set[String], message: ByteString)
            (implicit ec: ExecutionContext, session: DBSession = PlainMail.autoSession): Future[Unit] = Future {
    withSQL {
      insert.into(PlainMail).namedValues(
        column.randomId -> randomId,
        column.mailFrom -> mailFrom,
        column.recipients -> recipients.mkString(","),
        column.message -> message.decodeString("ascii")
      )
    }.execute().apply
  }
}
