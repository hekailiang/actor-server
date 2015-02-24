package im.actor.server.smtpd

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.data.message.rpc.messaging.TextMessage
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.persist.smtpd.PlainMail
import com.secretapp.backend.util.ACL
import im.actor.server.smtpd.internal.ApiBrokerActor
import scala.concurrent.duration._
import com.secretapp.backend.api.PhoneNumber
import org.apache.commons.mail.util.MimeMessageParser
import javax.mail.Session
import javax.mail.internet.{InternetAddress, MimeMessage}
import java.io.ByteArrayInputStream
import java.util.Properties
import scala.collection.JavaConversions._
import scala.concurrent.Future
import com.edlio.emailreplyparser._
import com.secretapp.backend.{ persist, models }
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.api.counters.CounterProtocol
import scala.collection.immutable
import scodec.bits.BitVector

object MailActor {
  def props(emailCounter: ActorRef, apiRouter: ActorRef) =
    Props(new MailActor(emailCounter, apiRouter))
}

case class EmailAddress(originalAddress: String, originailTitle: Option[String]) {
  val address: String = {
    val a =
      if (originalAddress.headOption == Some('+')) originalAddress.drop(1)
      else originalAddress
    a.replaceFirst("""\+.*@""", "@")
  }
  val alias = address.split("@").head
  val isValid = address.endsWith(SMTPServer.mailHost)
  val title = originailTitle.getOrElse(address)
  lazy val isPhoneNumber = PhoneNumber.isValid(alias, "RU")
}

object EmailAddress {
  def parse(ia: InternetAddress): EmailAddress = EmailAddress(ia.getAddress, Option(ia.getPersonal))
  def parse(a: String): EmailAddress = parse(new InternetAddress(a))
}

class MailActor(emailCounter: ActorRef, apiRouter: ActorRef)
  extends Actor with ActorLogging with GeneratorService {

  import context._

  implicit val timeout = Timeout(5.seconds)

  def getUser(mailFrom: EmailAddress): Future[models.User] = {
    persist.UserEmail.getUser(mailFrom.address).flatMap {
      case Some(u) => Future.successful(u)
      case None =>
        val userId = rand.nextInt(java.lang.Integer.MAX_VALUE) + 1 // TODO
        val emailId = rand.nextInt(java.lang.Integer.MAX_VALUE) + 1

        val user = models.User(
          uid = userId,
          authId = 0L,
          publicKeyData = BitVector.empty,
          publicKeyHash = 0L,
          phoneNumber = 0L,
          accessSalt = genAccessSalt,
          name = mailFrom.title,
          sex = models.NoSex,
          countryCode = "RU",
          phoneIds = immutable.Set(),
          emailIds = immutable.Set(emailId),
          state = models.UserState.Email,
          publicKeyHashes = immutable.Set()
        )

        for {
          _ <- persist.User.create(
            id = user.uid,
            accessSalt = user.accessSalt,
            name = user.name,
            countryCode = user.countryCode,
            sex = user.sex,
            state = user.state
          )(
            authId = user.authId,
            publicKeyHash = user.publicKeyHash,
            publicKeyData = user.publicKeyData
          )
          _ <- persist.UserEmail.create(
            id = emailId,
            userId = userId,
            accessSalt = genAccessSalt,
            email = mailFrom.address,
            title = mailFrom.title
          )
        } yield user
      }
  }

  def getRecipientUsers(recipients: Set[EmailAddress]): Future[Set[models.User]] = {
    val (phoneUserIds, emailUserIds) = recipients.partition(_.isPhoneNumber) match {
      case (phones, emails) => (
        Future.sequence(phones.map(_.alias.toLong).toSet.map(persist.UserPhone.findByNumber))
          .map(_.collect { case Some(p) => p.userId }),
        Future.sequence(emails.map(_.address).toSet.map(persist.UserEmail.findByEmail))
          .map(_.collect { case Some(ue) => ue.userId })
      )
    }
    Future.fold(Seq(phoneUserIds, emailUserIds))(Set[Int]())(_ ++ _).flatMap { userIds =>
      Future.sequence(userIds.map(persist.User.find(_)(None))).map(_.collect { case Some(u) => u }) // TODO: User.find worst code I've seen
    }
  }

  def receive = {
    case session: SMTPSession =>
      val randomId = rand.nextLong()
      persist.smtpd.PlainMail.create(randomId, session.mailFrom, session.recipients, session.message)

      val mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
        new ByteArrayInputStream(session.message.toArray))
      val parser = new MimeMessageParser(mimeMessage).parse()

      val mailFrom: EmailAddress = Option(parser.getFrom).map(EmailAddress.parse)
        .getOrElse(EmailAddress(session.mailFrom, None))
      val recipients: Set[EmailAddress] = Option(parser.getTo)
        .map(_.collect { case a: InternetAddress => EmailAddress.parse(a) }.toSet)
        .getOrElse(session.recipients.map(EmailAddress(_, None)))
      log.info(s"recipients: $recipients")
      val validRecipients = recipients.filter(_.isValid)
      if (validRecipients.nonEmpty) {
        log.debug(s"mailFrom: $mailFrom, recipients: $validRecipients")
        val emailParser = new EmailParser().parse(parser.getPlainContent)
        log.debug(s"emailParser: ${emailParser.getFragments}\ngetHiddenText:${emailParser.getHiddenText}\ngetVisibleText:${emailParser.getVisibleText}")
        log.debug(s"PhoneNumber: ${validRecipients.map { e => (e.alias, e.isPhoneNumber) }}")
        for {
          user <- getUser(mailFrom)
          recipientUsers <- getRecipientUsers(validRecipients)
        } yield {
          log.debug(s"user: $user, recipientUsers: $recipientUsers")

          recipientUsers.filterNot(_.uid == user.uid).foreach { recipient =>
            apiRouter ! ApiBrokerActor.AddContact(recipient, user)
            apiRouter ! ApiBrokerActor.SendMessage(
              currentUser = user,
              outPeer = struct.OutPeer.privat(recipient.uid, ACL.userAccessHash(user.authId, recipient)),
              randomId = randomId,
              message = TextMessage(emailParser.getVisibleText)
            )
          }
        }
      } else log.error(s"invalid recipients: $recipients")
  }
}
