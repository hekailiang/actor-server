package im.actor.server.smtpd

import java.io.FileInputStream

import akka.actor._
import akka.stream.StreamSubscriptionTimeoutTerminationMode.CancelTermination
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnNext}
import akka.stream.actor.{ActorPublisher, OneByOneRequestStrategy, RequestStrategy, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{FlowMaterializer, StreamSubscriptionTimeoutSettings, MaterializerSettings}
import akka.stream.ssl.{SslTlsCipherActor, SslTlsCipher}
import akka.stream.ssl.SslTlsCipher.{InboundSession, SessionNegotiation}
import akka.util.ByteString
import javax.net.ssl._
import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.Promise
import scala.concurrent.duration._

object TLSActor {
  def props(connection: ActorRef, keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory, timeout: FiniteDuration) = Props(new TLSActor(connection, keyManagerFactory, trustManagerFactory, timeout))

  def getManagerFactories() = {
    val config = SMTPServer.config.getConfig("certificate")
    val password = config.getString("password").toCharArray
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(new FileInputStream(config.getString("keystore")), password)
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(keyStore, password)
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(keyStore)
    (keyManagerFactory, trustManagerFactory)
  }

  def sslEngine(keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory): SSLEngine = {
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

    val sslEngine = context.createSSLEngine()
    val sslEnabledProtocols = context.getSupportedSSLParameters.getProtocols.filterNot(_.startsWith("SSLv3"))
    sslEngine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_128_CBC_SHA"))
    sslEngine.setEnabledProtocols(sslEnabledProtocols)
    sslEngine.setUseClientMode(false)
    sslEngine
  }

  def sessionNegotiation(keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory) =
    SessionNegotiation(sslEngine(keyManagerFactory, trustManagerFactory))

  case class Wrap(bs: ByteString) // plain text to TLS stream
  case class Unwrap(bs: ByteString) // raw TLS stream from client
  case class Wrapped(bs: ByteString) // raw TLS stream to client
  case class Unwrapped(bs: ByteString) // plain text from client raw TLS stream
}

private class ByteStringStream extends ActorPublisher[ByteString] with ActorLogging {
  def receive = {
    case bs: ByteString => onNext(bs)
  }
}

private class WrappedSubscriber(connection: ActorRef) extends ActorSubscriber with ActorLogging {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive = {
    case OnNext(bs: ByteString) => connection ! TLSActor.Wrapped(bs)
  }
}

private class UnwrappedSubscriber(connection: ActorRef) extends ActorSubscriber with ActorLogging {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive = {
    case OnNext(bs: ByteString) => connection ! TLSActor.Unwrapped(bs)
  }
}

private class TLSSessionActor(unwrappedActor: ActorRef) extends ActorSubscriber with ActorLogging {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive = {
    case OnNext(session: InboundSession) => session.data.subscribe(ActorSubscriber(unwrappedActor))
  }
}

class TLSActor(connection: ActorRef, keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory, timeout: FiniteDuration) extends Actor with ActorLogging {
  implicit val system = context.system
  implicit val ec = system.dispatcher
  implicit val flow = FlowMaterializer()

  context.setReceiveTimeout(timeout)

  val tlsCipher = Promise[SslTlsCipher]()
  var tlsBuffer = ByteString.empty

  val plainTextActor = context.actorOf(Props(new ByteStringStream), "plain-text-stream")
  val cipherTextActor = context.actorOf(Props(new ByteStringStream), "cipher-text-stream")
  val cipherTextSubscriber = context.actorOf(Props(new WrappedSubscriber(connection)), "cipher-text-subscriber")
  val plainTextSubscriber = context.actorOf(Props(new UnwrappedSubscriber(connection)), "plain-text-subscriber")
  val tlsSessionActor = context.actorOf(Props(new TLSSessionActor(plainTextSubscriber)), "tls-session")

  override def preStart(): Unit = {
    super.preStart()
    log.debug("start TLS")
    val props = Props(new SslTlsCipherActor(self, TLSActor.sessionNegotiation(keyManagerFactory, trustManagerFactory), true) {
      // override val subscriptionTimeoutSettings = StreamSubscriptionTimeoutSettings(mode = CancelTermination, timeout = timeout)
    })
    val tlsActor = context.actorOf(props, "tls-cipher")

    for {
      ref <- Array(tlsActor, plainTextActor, cipherTextActor, cipherTextSubscriber, plainTextSubscriber, tlsSessionActor)
    } yield context.watch(ref)
  }

  def receive = {
    case TLSActor.Unwrap(data) =>
      log.debug(s"Unwrap: $data, ${data.utf8String}")
      if (tlsCipher.isCompleted) cipherTextActor ! data
      else tlsBuffer ++= data
    case TLSActor.Wrap(data) =>
      log.debug(s"Wrap: $data, ${data.utf8String}")
      plainTextActor ! data
    case cipher: SslTlsCipher =>
      log.debug(s"cipher: $cipher")

      Source(ActorPublisher(cipherTextActor)).to(Sink(cipher.cipherTextInbound)).run()
      Source(ActorPublisher(plainTextActor)).to(Sink(cipher.plainTextOutbound)).run()
      Source(cipher.cipherTextOutbound).to(Sink(ActorSubscriber[ByteString](cipherTextSubscriber))).run()

      cipher.sessionInbound.subscribe(ActorSubscriber(tlsSessionActor))

      tlsCipher.success(cipher)

      if (tlsBuffer.nonEmpty) {
        val data = tlsBuffer
        tlsBuffer = ByteString.empty
        cipherTextActor ! data
      }
    case ReceiveTimeout =>
      log.debug("Shutdown by timeout")
      context.stop(self)
  }
}
