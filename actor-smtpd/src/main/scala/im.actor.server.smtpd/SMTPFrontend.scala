package im.actor.server.smtpd

import java.net.InetSocketAddress

import akka.actor._
import akka.util.ByteString

import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.{ Try, Success }

object SMTPFrontend {
  type InterfaceTuple = (immutable.Set[String], immutable.Set[String])

  val sizeLimit = 10485760

  def props(connection: ActorRef, remote: InetSocketAddress, hostname: String, mailRouter: ActorRef, tlsConnection: Boolean) = {
    Props(new SMTPFrontend(connection, remote, hostname, mailRouter, tlsConnection))
  }

  val banner = s"${SMTPServer.hostname} ESMTP"
  val SMTPExtensions = List(
    s"SIZE $sizeLimit",
    "8BITMIME",
    "STARTTLS",
    "ENHANCEDSTATUSCODES",
    "PIPELINING",
    "CHUNKING",
    "SMTPUTF8",
    "DSN")

  private def ehloBanner(ext: List[String]) =
    (List(s"250-$banner") ++ ext.dropRight(1).map(e => s"250-$e") ++ List(s"250 ${ext.last}")).mkString("\r\n")

  private object SMTPResponse {
    val welcome = ByteString(s"220 $banner\r\n")
    val ehloTLS = ByteString(s"${ehloBanner(SMTPExtensions)}\r\n")
    val ehloWithoutTLS = ByteString(s"${ehloBanner(SMTPExtensions.filterNot(_ == "STARTTLS"))}\r\n")
    val helo = ByteString(s"250 $banner\r\n")
    val endData = ByteString("354 End data with <CR><LF>.<CR><LF>\r\n")
    val alreadyTLS = ByteString("530 There's TLS already\r\n")
    val goAhead = ByteString("220 2.0.0 Go ahead\r\n")
    val bye = ByteString("221 2.0.0 Bye\r\n")
    val unknown = ByteString("500 Unknown command\r\n")
    val ok = ByteString("250 2.0.0 Ok\r\n")
    val messageTooBig = ByteString("552 5.3.4 Error: message file too big\r\n")
    val invalidSize = ByteString("501 Size is invalid\r\n")
    val invalidAddressOperand = ByteString("501 MAIL must have an address operand\r\n")
    val noHostname = ByteString("501 No hostname\r\n")
    val addressOk = ByteString("250 2.1.5 Ok\r\n")
    val addressSyntaxError = ByteString("501 5.5.4 Syntax error\r\n")
    val addressBadSender = ByteString("501 5.1.7 Bad sender address syntax\r\n")
    val wrongTransition = ByteString("503 5.5.1 Wrong transition\r\n")
  }

  private object SMTPPart {
    val dot = ByteString(".")
    val to = ByteString("TO:")
    val from = ByteString("FROM:")
  }
}

class SMTPFrontend(connection: ActorRef, remote: InetSocketAddress, hostname: String, mailRouter: ActorRef, tlsConnection: Boolean) extends Actor with ActorLogging {
  import akka.io.Tcp._
  import SMTPFrontend.{ SMTPResponse, SMTPPart }
  import context._

  val timeout = 5.seconds

  context.setReceiveTimeout(timeout) // TODO

  type SessionState = (MailState.MailState, Option[SMTPSession])

  val beginState = (MailState.BEGIN, None)

  def write(bs: ByteString): Unit = {
    log.debug(s"write(bs): ${bs.utf8String}")
    if (tlsActor.isCompleted) tlsActorF.map(_ ! TLSActor.Wrap(bs))
    else connection ! Write(bs)
  }

  @inline
  def writeWithState(bs: ByteString, state: SessionState): SessionState = {
    write(bs)
    state
  }

  override def preStart(): Unit = {
    super.preStart()
    write(SMTPResponse.welcome)
  }

  val NL: Byte = '\n'

  @tailrec
  final def readLine(bs: ByteString, session: SessionState, f: (SessionState, ByteString) => SessionState): (SessionState, ByteString) = {
    if (bs.nonEmpty) {
      val line = bs.takeWhile(_ != NL)
      if (line.length == bs.length) (session, bs)
      else {
        val nextState =
          if (session._1 == MailState.DATA) f(session, line :+ NL)
          else if (line.lastOption == Some('\r')) f(session, line.dropRight(1))
          else f(session, line)

        if (nextState._1 == MailState.CLOSE) {
          log.info("CLOSE !!!")
          connection ! Close
          ((MailState.CLOSE, None), ByteString.empty)
        } else {
          val tail = bs.drop(line.length + 1)
          readLine(tail, nextState, f)
        }
      }
    } else (session, ByteString.empty)
  }

  @inline
  def toUpperCase(bs: ByteString) = bs.mapI { c => if (c >= 97 && c <= 122) c - 32 else c }

  def parseEmail(bs: ByteString, arg: ByteString, state: SessionState)(f: (String, Array[String]) => SessionState): SessionState = {
    val line = bs.dropWhile(_ == ' ')
    if (toUpperCase(line.take(arg.length)) == arg) {
      val args = line.drop(arg.length).decodeString("ascii").split(' ').filterNot(_.isEmpty)
      if (args.isEmpty) writeWithState(SMTPResponse.invalidAddressOperand, state)
      else {
        Regex.email.findFirstIn(args.head.replaceAll("""\A\<*|\>*\z""", "")) match {
          case Some(email) => writeWithState(SMTPResponse.addressOk, f(email, args.tail))
          case None => writeWithState(SMTPResponse.addressBadSender, state)
        }
      }
    } else writeWithState(SMTPResponse.addressSyntaxError, state)
  }

  @inline
  def parseHostname(bs: ByteString, response: ByteString) = {
    val hostname = bs.dropWhile(_ == ' ')
    if (hostname.nonEmpty) write(response)
    else write(SMTPResponse.noHostname)
  }

  @inline
  def transition(state: SessionState, nextState: MailState.MailState)(f: => SessionState): SessionState = {
    if (MailState.states.contains(state._1 -> nextState)) f
    else writeWithState(SMTPResponse.wrongTransition, state)
  }

  def trimBS(bs: ByteString) = bs.takeWhile(c => c != '\r' && c != '\n')

  def handleSession(state: SessionState, data: ByteString): SessionState = {
    if (state._1 == MailState.DATA) {
      if (trimBS(data) == SMTPPart.dot) {
        log.info(s"complete SMTP session: ${state._2}")
        mailRouter ! state._2.get
        writeWithState(SMTPResponse.ok, beginState)
      } else {
        val trimData = trimBS(data)
        val line = if (trimData.length > 1 && trimData.forall(_ == '.')) data.tail else data
        if (state._2.exists(s => (s.message.length + line.length) > SMTPFrontend.sizeLimit)) {
          writeWithState(SMTPResponse.messageTooBig, beginState)
        } else {
          val newState = state._2.map { s => s.copy(message = s.message ++ line) }
          (MailState.DATA, newState)
        }
      }
    } else {
      val command = toUpperCase(data.takeWhile(c => (c >= 65 && c <= 90) || (c >= 97 && c <= 122)))
      val arguments = data.drop(command.length + 1)
      command match {
        case SmtpCommands.EHLO =>
          parseHostname(arguments, if (tlsConnection) SMTPResponse.ehloWithoutTLS else SMTPResponse.ehloTLS)
          beginState
        case SmtpCommands.HELO =>
          parseHostname(arguments, SMTPResponse.helo)
          beginState
        case SmtpCommands.MAIL =>
          log.debug(s"mail: ${arguments.utf8String}")
          transition(state, MailState.MAIL) {
            parseEmail(arguments, SMTPPart.from, state) { (email, args) =>
              log.debug(s"from is ok, email: $email")
              val res = (MailState.MAIL, Some(SMTPSession(email, Set(), ByteString.empty)))
              val size = args.find(_.startsWith("SIZE=")).map { arg => Try(arg.drop(5).toInt) }
              size match {
                case Some(Success(n)) if n >= 0 && n <= SMTPFrontend.sizeLimit => res
                case None => res
                case _ => writeWithState(SMTPResponse.invalidSize, state)
              }
            }
          }
        case SmtpCommands.RCPT =>
          log.debug(s"rcpt: ${arguments.utf8String}")
          transition(state, MailState.RCPT) {
            parseEmail(arguments, SMTPPart.to, state) { (email, _) =>
              log.debug(s"to is ok, email: $email")
              (MailState.RCPT, state._2.map { s => s.copy(recipients = s.recipients + email) })
            }
          }
        case SmtpCommands.DATA =>
          log.debug(s"data: $arguments")
          transition(state, MailState.DATA) { writeWithState(SMTPResponse.endData, (MailState.DATA, state._2)) }
        case SmtpCommands.RSET =>
          log.debug(s"rset: $arguments")
          writeWithState(SMTPResponse.ok, beginState)
        case SmtpCommands.STARTTLS =>
          if (tlsConnection || tlsActor.isCompleted) writeWithState(SMTPResponse.alreadyTLS, state)
          else {
            log.debug(s"starttls: $arguments")
            write(SMTPResponse.goAhead)
            tlsActor.success(context.actorOf(TLSActor.props(self, timeout), "tls-actor"))
            state
          }
        case SmtpCommands.QUIT =>
          log.debug(s"quit: $arguments")
          writeWithState(SMTPResponse.bye, (MailState.CLOSE, state._2))
        case m =>
          log.debug(s"unknown: ${m.utf8String}")
          writeWithState(SMTPResponse.unknown, state)
      }
    }
  }

  var tlsActor = Promise[ActorRef]()
  var tlsActorF = tlsActor.future
  var sessionState: (SessionState, ByteString) = (beginState, ByteString.empty)

  @inline
  def applySessionState(bs: ByteString): Unit = {
    log.debug(s"applySessionState: ${bs.utf8String}")
    sessionState = readLine(sessionState._2 ++ bs, sessionState._1, handleSession)
    log.debug(s"sessionState: $sessionState")
  }

  def receive = {
    case Received(data) =>
      log.debug(s"Received: $data, ${data.utf8String}")
      if (tlsActor.isCompleted) tlsActorF.map(_ ! TLSActor.Unwrap(data))
      else applySessionState(data)
    case TLSActor.Wrapped(bs) =>
      log.debug(s"Wrapped: $bs, ${bs.utf8String}")
      connection ! Write(bs)
    case TLSActor.Unwrapped(bs) =>
      log.debug(s"Unwrapped: $bs, ${bs.utf8String}")
      applySessionState(bs)
    case _: ConnectionClosed =>
      context.stop(self)
    case ReceiveTimeout =>
      log.debug("Close connection by timeout")
      connection ! Close
  }
}

