package org.specs2.mutable

import akka.actor._
import akka.io.Tcp.{ Close, Received, Write }
import akka.testkit.{ TestKitBase, TestProbe }
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.Singletons
import com.secretapp.backend.api.frontend.{ MTConnection, TransportConnection }
import com.secretapp.backend.api.frontend.tcp.TcpFrontend
import com.secretapp.backend.api.frontend.ws.WSFrontend
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.transport.MTPackageBoxCodec
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.session._
import com.websudos.util.testing._
import java.net.InetSocketAddress
import java.security.{ KeyPairGenerator, SecureRandom, Security }
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import org.specs2.specification.Fragment
import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import spray.can.websocket._

trait ActorServiceHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers =>

  Security.addProvider(new BouncyCastleProvider())

  val mockAuthId = rand.nextLong()
  val defaultPhoneNumber = 79853867016L

  protected var incMessageId = 0L

  val singletons = new Singletons
  val sessionReceiveTimeout = singletons.appConfig.getDuration("session.receive-timeout", MILLISECONDS)
  val sessionRegion = SessionActor.startRegion(singletons, sessionReceiveTimeout.milliseconds)(system, csession)

  def genPhoneNumber() = {
    79853867016L + rand.nextInt(10000000)
  }

  def genPublicKey = {
    val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
    val g = KeyPairGenerator.getInstance("ECDSA", "BC")
    g.initialize(ecSpec, new SecureRandom())
    val pair = g.generateKeyPair()
    val pubKey = pair.getPublic.asInstanceOf[ECPublicKey]
    BitVector(pubKey.getQ.getEncoded)
  }

  def insertAuthId(authId: Long, userId: Option[Int] = None): Unit = blocking {
    persist.AuthId.insertEntity(models.AuthId(authId, userId)).sync()
  }

  def addUser(authId: Long, sessionId: Long, u: models.User, phoneNumber: Long): Unit = blocking {
    persist.AuthId.insertEntity(models.AuthId(authId, None)).sync()
    persist.User.insertEntityWithChildren(u).sync()
  }

  def authUser(u: models.User, phoneNumber: Long): models.User = blocking {
    insertAuthId(u.authId, u.uid.some)
    persist.User.insertEntityWithChildren(u).sync()
    u
  }

  def authDefaultUser(userId: Int, phoneNumber: Long = defaultPhoneNumber)(implicit destActor: ActorRef, s: SessionIdentifier, authId: Long): models.User = blocking {
    val publicKey = hex"ac1d".bits
    val name = s"Timothy${userId} Klim${userId}"
    val pkHash = ec.PublicKey.keyHash(publicKey)
    val user = models.User(userId, authId, pkHash, publicKey, phoneNumber, "salt", name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
    authUser(user, phoneNumber)
  }

  val smsCode = "test_sms_code"
  val smsHash = "test_sms_hash"
  val userId = 101
  val userSalt = "user_salt"

  private def inetAddr = new InetSocketAddress("localhost", 0)

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
    override def genSmsCode = smsCode
    override def genSmsHash = smsHash
    override def genUserId = userId
    override def genUserAccessSalt = userSalt
  }

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new TcpFrontend(probe.ref, inetAddr, sessionRegion, csession) with GeneratorServiceMock))
    (probe, actor)
  }

  def genTestScope()(implicit transport: TransportConnection): TestScopeNew = {
    val (probe, apiActor) = getProbeAndActor()
    TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(), authId = rand.nextLong())
  }

  def genTestScopeWithUser()(implicit transport: TransportConnection) = {
    val scope = genTestScope()
    val userId = rand.nextInt()
    val phoneNumber = genPhoneNumber()
    val publicKey = BitVector(rand.nextString(100).getBytes)
    val salt = rand.nextString(25)
    val name = s"Timothy$userId Klim$userId"
    val pkHash = ec.PublicKey.keyHash(publicKey)
    val userStruct = models.User(userId, scope.authId, pkHash, publicKey, phoneNumber, salt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
    val user = authUser(userStruct, phoneNumber)
    scope.copy(userOpt = user.some)
  }

  def getProbeAndActor()(implicit transport: TransportConnection) = {
    val probe = TestProbe()
    val actor = transport match {
      case MTConnection => system.actorOf(TcpFrontend.props(probe.ref, inetAddr, sessionRegion, csession))
    }
    (probe, actor)
  }

  case class TestScope(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, user: models.User) {
    def reconnect(): TestScope = {
      val (probe, actor) = probeAndActor()
      this.copy(probe = probe, apiActor = actor)
    }
  }

  object TestScope {
    def pair(): (TestScope, TestScope) = {
      pair(1, 2)
    }

    def pair(uid1: Int, uid2: Int): (TestScope, TestScope) = {
      (apply(uid1, 79632740769L), apply(uid2, 79853867016L))
    }

    def apply(): TestScope = apply(1, 79632740769L)

    def apply(userId: Int): TestScope = apply(userId, 79632740769L)

    def apply(userId: Int, phone: Long): TestScope = {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      implicit val authId = rand.nextLong()
      val publicKey = BitVector.fromLong(rand.nextLong())
      val pkHash = ec.PublicKey.keyHash(publicKey)
      val newUser = models.User(userId, authId, pkHash, publicKey, phone, "salt", s"Timothy_$userId Klim_$userId", "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
      val user = authUser(newUser, phone)
      TestScope(probe, apiActor, session, user)
    }
  }

  case class WrappedReceivePackage(f: (Long) => Unit)

  implicit class WrappedTM(m: TransportMessage) {
    def :~>(wp: WrappedReceivePackage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, authId: Long): Unit = {
      val r = pack(0, authId, m) // TODO: real index
      probe.send(destActor, Received(r.blob))
      wp.f(r.messageId)
    }
  }

  def tcpReceiveN(n: Int, duration: FiniteDuration = 5.seconds)(implicit probe: TestProbe, destActor: ActorRef) = {
    @annotation.tailrec
    def receive(acc: immutable.Seq[Object] = immutable.Seq.empty): immutable.Seq[Object] = {
      // TODO: move to argument
      probe.receiveN(1, duration).head match {
        case message @ Write(data, ack) =>
          probe.send(destActor, ack)

          if (acc.length + 1 < n) {
            receive(acc :+ message)
          } else {
            acc :+ message
          }
        case Close => receive(acc)
        case x =>
          println(s"unknown receive $x")
          receive(acc)
      }

    }

    receive()
  }

  def protoReceiveN(n: Int, duration: FiniteDuration = 5.seconds)(implicit probe: TestProbe, destActor: ActorRef): immutable.Seq[MTPackage] = {
    tcpReceiveN(n, duration) map {
      case Write(data, _) =>
        val p = MTPackageBoxCodec.decodeValidValue(data).p
        // FIXME: real index
        val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)
        val pb = MTPackageBox(0, MTPackage(p.authId, p.sessionId,
          MessageBoxCodec.encodeValid(MessageBox(getMessageId(), MessageAck(Vector(mb.messageId))))))
        probe.send(destActor, Received(ByteString(MTPackageBoxCodec.encodeValid(pb).toByteArray)))
        //Thread.sleep(200) // let acktracker handle the ack
        p
    }
  }

  def @<~:(m: TransportMessage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, authId: Long): WrappedReceivePackage = WrappedReceivePackage { (messageId: Long) =>
    val received = protoReceiveN(3)

    val res = MTPackage(authId, s.id, MessageBoxCodec.encodeValid(MessageBox(messageId, m)))
    val ack = MTPackage(authId, s.id, MessageBoxCodec.encodeValid(MessageBox(messageId * 10, MessageAck(Vector(messageId))))) // TODO: move into DSL method
    val ses = MTPackage(authId, s.id, MessageBoxCodec.encodeValid(MessageBox(messageId, NewSession(s.id, messageId))))
    val expectedMsg = Set(ses, ack, res)

    received.toSet should equalTo(expectedMsg)
  }

  case class PackResult(blob: ByteString, messageId: Long)

  def pack(index: Int, authId: Long, m: MessageBox)(implicit s: SessionIdentifier): PackResult = {
    pack(index, authId, m.body, m.messageId)
  }

  def getMessageId(): Long = {
    incMessageId += 4
    incMessageId
  }

  def pack(index: Int, authId: Long, m: TransportMessage, messageId: Long = getMessageId())(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, m)
    PackResult(codecRes2BS(p), messageId)
  }

  def ack(index: Int, messageId: Long, authId: Long)(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, MessageAck(Vector(messageId)))
    PackResult(codecRes2BS(p), messageId)
  }

  /**
   * Receive message boxes ignoring acks
   */
  def receiveNMessageBoxes(n: Int, packages: Seq[MessageBox] = Seq.empty)(implicit probe: TestProbe, destActor: ActorRef): Seq[MessageBox] = {
    val receivedPackages = protoReceiveN(n)

    val mboxes = receivedPackages flatMap {
      case MTPackage(authId, sessionId, mboxBytes) =>
        val messageBox = MessageBoxCodec.decodeValue(mboxBytes).toOption.get

        messageBox match {
          case MessageBox(_, Container(mbox)) => mbox
          case mb @ MessageBox(_, _) => Seq(mb)
        }
    }
    val (ackPackages, newPackages) = mboxes partition {
      case MessageBox(_, ma @ MessageAck(_)) => true
      case _ => false
    }

    if (newPackages.length + packages.length == n) {
      packages ++ newPackages
    } else {
      receiveNMessageBoxes(n - packages.length - newPackages.length, packages ++ newPackages)
    }
  }

  // TODO: remove this hack
  def catchNewSession(scope: TestScope): Unit = {
    catchNewSession()(scope.probe, scope.apiActor, scope.session, scope.user.authId)
  }

  def catchNewSession()(implicit probe: TestProbe, apiActor: ActorRef, s: SessionIdentifier, authId: Long): Unit = {
    val messageId = getMessageId()
    val packageBlob = pack(0, authId, MessageBox(messageId, Ping(0L)))(s)
    send(packageBlob)(probe, apiActor)
    val mboxes = protoReceiveN(3)(probe, apiActor) map {
      case MTPackage(_, _, mboxBytes) => MessageBoxCodec.decodeValue(mboxBytes).toOption.get.body
    }
    val score = mboxes.map {
      case _: NewSession => 1
      case _: Pong => 3
      case _: MessageAck => 7
      case _ => 0
    }.fold(0) { _ + _ }
    if (score != 11) throw new Exception("something want wrong while catchNewSession...")
  }

  /*
   * Receive message boxes which should come in response to requests, include updates, ignore acks
   * If m updates came n + m message boxes returned.
   */
  def receiveNResponses(n: Int, packages: Seq[MessageBox] = Seq.empty)(implicit probe: TestProbe, destActor: ActorRef): Seq[MessageBox] = {
    val receivedPackages = protoReceiveN(n)

    val mboxes = receivedPackages flatMap {
      case MTPackage(authId, sessionId, mboxBytes) =>
        val messageBox = MessageBoxCodec.decodeValue(mboxBytes).toOption.get

        messageBox match {
          case MessageBox(_, Container(mbox)) => mbox
          case mb @ MessageBox(_, _) => Seq(mb)
        }
    }
    val (ackPackages, newPackages) = mboxes partition {
      case MessageBox(_, ma @ MessageAck(_)) => true
      case _ => false
    }

    newPackages filter {
      case MessageBox(_, ma: UpdateBox) =>
        true
      case _ => false
    } length match {
      case 0 =>
        packages ++ newPackages
      case updatesCount =>
        receiveNResponses(n - newPackages.length - ackPackages.length, packages ++ newPackages)
    }
  }

  def receiveNWithAck(n: Int)(implicit probe: TestProbe, destActor: ActorRef): Seq[MessageBox] = {
    receiveNResponses(n * 2)
  }

  def receiveOneWithAck()(implicit probe: TestProbe, destActor: ActorRef): MessageBox = {
    receiveNWithAck(1) match {
      case Seq(x) => x
      case Seq() => null
      case x => throw new Exception(s"Received more than one message ${x}")
    }
  }

  def expectMsgWithAck(messages: MessageBox*)(implicit probe: TestProbe, destActor: ActorRef) = {
    // TODO: DRY (receiveNWithAck)

    val receivedPackages = protoReceiveN(messages.size * 2)

    val mboxes = receivedPackages flatMap {
      case MTPackage(authId, sessionId, mboxBytes) =>
        val messageBox = MessageBoxCodec.decodeValue(mboxBytes).toOption.get

        messageBox match {
          case MessageBox(_, Container(mbox)) => mbox
          case mb @ MessageBox(_, _) => Seq(mb)
        }
    }
    val (ackPackages, expectedPackages) = mboxes partition {
      case MessageBox(_, ma @ MessageAck(_)) => true
      case _ => false
    }
    val acks = ackPackages flatMap (a => a.body.asInstanceOf[MessageAck].messageIds)
    val expectedMsgIds = messages.map(_.messageId)
    expectedMsgIds.length should_== acks.length
    expectedMsgIds should containAllOf(acks)
    expectedPackages.length should_== messages.length
    expectedPackages should containAllOf(messages)
  }

  def send(r: PackResult)(implicit probe: TestProbe, destActor: ActorRef) {
    probe.send(destActor, Received(r.blob))
  }
}
