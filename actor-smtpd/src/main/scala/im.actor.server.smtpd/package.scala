package im.actor.server.smtpd

import akka.util.ByteString
import scala.collection.immutable

object Regex {
  val email = """\A([a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)\z""".r
}

object SmtpCommands {
  val EHLO = ByteString("EHLO")
  val HELO = ByteString("HELO")
  val MAIL = ByteString("MAIL")
  val RCPT = ByteString("RCPT")
  val DATA = ByteString("DATA")
  val QUIT = ByteString("QUIT")
  val RSET = ByteString("RSET")
  val NOOP = ByteString("NOOP")
  val STARTTLS = ByteString("STARTTLS")
}

@SerialVersionUID(1L)
case class SMTPSession(mailFrom: String, recipients: Set[String], message: ByteString)

object MailState extends Enumeration {
  type MailState = Value
  val BEGIN, MAIL, RCPT, DATA, CLOSE = Value

  val states = immutable.HashSet(
    MailState.BEGIN -> MailState.MAIL,
    MailState.MAIL -> MailState.RCPT,
    MailState.RCPT -> MailState.RCPT,
    MailState.RCPT -> MailState.DATA)
}
