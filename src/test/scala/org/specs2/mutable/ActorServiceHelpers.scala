package org.specs2.mutable

import akka.actor._
import akka.io.Tcp.{ Received, Write }
import akka.testkit.{ TestKitBase, TestProbe }
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.api.{ ClusterProxies, Singletons }
import com.secretapp.backend.data.message.{ Container, MessageAck, NewSession, TransportMessage }
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.{ MessageBox, Package, PackageBox }
import com.secretapp.backend.persist.{ AuthIdRecord, SessionIdRecord, UserRecord }
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.transport.{ PackageBoxCodec, TcpConnector }
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon.Authenticate
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.session._
import java.security.{ KeyPairGenerator, SecureRandom, Security }
import java.util.concurrent.atomic.AtomicLong
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.scalamock.specs2.MockFactory
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.Random
import scalaz._
import scalaz.Scalaz._
import scodec.bits._

trait ActorServiceHelpers extends RandomService {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers with MockFactory =>

  Security.addProvider(new BouncyCastleProvider())

  val mockAuthId = rand.nextLong()
  val defaultPhoneNumber = 79853867016L

  implicit val session: CSession

  protected val incMessageId = new AtomicLong(1L)

  def codecRes2BS(res: String \/ BitVector): ByteString = {
    ByteString(res.toOption.get.toByteBuffer)
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
    AuthIdRecord.insertEntity(AuthId(authId, userId)).sync()
  }

  def insertSessionId(authId: Long)(implicit s: SessionIdentifier): Unit = blocking {
    SessionIdRecord.insertEntity(SessionId(authId, s.id)).sync()
  }

  def insertAuthAndSessionId(authId: Long, userId: Option[Int] = None)(implicit s: SessionIdentifier): Unit = {
    insertAuthId(authId, userId)
    insertSessionId(authId)
  }

  def addUser(authId: Long, sessionId: Long, u: User, phoneNumber: Long): Unit = blocking {
    AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
    SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
  }

  def authUser(u: User, phoneNumber: Long)(implicit destActor: ActorRef, s: SessionIdentifier): User = blocking {
    insertAuthAndSessionId(u.authId, u.uid.some)
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
    destActor ! Authenticate(u)
    u
  }

  def authDefaultUser(uid: Int = 1, phoneNumber: Long = defaultPhoneNumber)(implicit destActor: ActorRef, s: SessionIdentifier, authId: Long): User = blocking {
    val publicKey = hex"ac1d".bits
    val name = s"Timothy${uid} Klim${uid}"
    val user = User.build(uid = uid, authId = authId, publicKey = publicKey, accessSalt = "salt",
      phoneNumber = phoneNumber, name = name)
    val accessHash = User.getAccessHash(authId, uid, "salt")
    authUser(user, phoneNumber)
  }

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong)
  }

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs () returning (12345L)
      }
    }
  }

  val smsCode = "test_sms_code"
  val smsHash = "test_sms_hash"
  val userId = 101
  val userSalt = "user_salt"

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
    override def genSmsCode = smsCode
    override def genSmsHash = smsHash
    override def genUserId = userId
    override def genUserAccessSalt = userSalt
  }

  val counters = new Singletons
  implicit val clusterProxies = new ClusterProxies
  val sessionRegion = SessionActor.startRegion()

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new TcpConnector(probe.ref, sessionRegion, session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

  case class TestScope(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, user: User)
  object TestScope {
    def pair(): (TestScope, TestScope) = {
      pair(1, 2)
    }

    def pair(uid1: Int, uid2: Int): (TestScope, TestScope) = {
      (apply(uid1, 79632740769L), apply(uid2, 79853867016L))
    }

    def apply(): TestScope = apply(1, 79632740769L)

    def apply(uid: Int, phone: Long): TestScope = {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      implicit val authId = rand.nextLong
      val user = authDefaultUser(uid)
      TestScope(probe, apiActor, session, user)
    }
  }

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  case class WrappedReceivePackage(f: (Long) => Unit)

  implicit class WrappedTM(m: TransportMessage) {
    def :~>(wp: WrappedReceivePackage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, authId: Long): Unit = {
      val r = pack(0, authId, m) // TODO: real index
      probe.send(destActor, Received(r.blob))
      wp.f(r.messageId)
    }
  }

  def tcpReceiveN(n: Int, duration: FiniteDuration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef) = {
    @annotation.tailrec
    def receive(acc: immutable.Seq[Object] = immutable.Seq.empty): immutable.Seq[Object] = {
      // TODO: move to argument
      probe.receiveN(1, duration).head match {
        case message @ Write(data, ack) =>
          println("acking")
          probe.send(destActor, ack)

          if (acc.length + 1 < n) {
            receive(acc :+ message)
          } else {
            acc :+ message
          }
        case x =>
          println(s"unknown receive ${x}")
          receive(acc)
      }

    }

    receive()
  }

  def protoReceiveN(n: Int, duration: FiniteDuration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef) = {
    tcpReceiveN(n, duration) map {
      case Write(data, _) => PackageBoxCodec.decodeValidValue(data).p
    }
  }

  def @<~:(m: TransportMessage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, authId: Long): WrappedReceivePackage = WrappedReceivePackage { (messageId: Long) =>
    val received = protoReceiveN(3)

    val res = Package(authId, s.id, MessageBox(messageId, m))
    val ack = Package(authId, s.id, MessageBox(messageId * 10, MessageAck(Vector(messageId)))) // TODO: move into DSL method
    val ses = Package(authId, s.id, MessageBox(messageId, NewSession(s.id, messageId)))
    val expectedMsg = Set(ses, ack, res)

    received.toSet should equalTo(expectedMsg)
  }

  case class PackResult(blob: ByteString, messageId: Long)

  def pack(index: Int, authId: Long, m: MessageBox)(implicit s: SessionIdentifier): PackResult = {
    pack(index, authId, m.body, m.messageId)
  }

  def pack(index: Int, authId: Long, m: TransportMessage, messageId: Long = incMessageId.incrementAndGet())(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, m)
    PackResult(codecRes2BS(p), messageId)
  }

  def ack(index: Int, messageId: Long, authId: Long)(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, MessageAck(Vector(messageId)))
    PackResult(codecRes2BS(p), messageId)
  }

  def receiveNWithAck(n: Int)(implicit probe: TestProbe, destActor: ActorRef): Seq[MessageBox] = {
    val receivedPackages = tcpReceiveN(n * 2)
    val unboxed = for (p <- receivedPackages) yield {
      p match {
        case Write(bs, ack) =>
          protoPackageBox.decode(bs) match {
            case -\/(e) =>
              throw new Exception(s"Cannot decode PackageBox $e")
            case \/-(p) =>
              p._2
          }
      }
    }
    val mboxes = unboxed flatMap {
      case PackageBox(_, Package(_, _, MessageBox(_, Container(mbox)))) => mbox
      case PackageBox(_, Package(_, _, mb @ MessageBox(_, _))) => Seq(mb)
    }
    val (ackPackages, packages) = mboxes partition {
      case MessageBox(_, ma @ MessageAck(_)) => true
      case _ => false
    }
    ackPackages.length should equalTo(packages.length)
    packages
  }

  def receiveOneWithAck()(implicit probe: TestProbe, destActor: ActorRef): MessageBox = {
    receiveNWithAck(1) match {
      case Seq(x) => x
      case Seq() => null
      case _ => throw new Exception("Received more than one message")
    }
  }

  def expectMsgWithAck(messages: MessageBox*)(implicit probe: TestProbe, destActor: ActorRef) = {
    val receivedPackages = tcpReceiveN(messages.size * 2)
    val unboxed = for (p <- receivedPackages) yield {
      p match {
        case Write(bs, ack) =>
          protoPackageBox.decode(bs) match {
            case -\/(e) =>
              throw new Exception(s"Cannot decode PackageBox $e")
            case \/-(p) =>
              p._2
          }
      }
    }
    val mboxes = unboxed flatMap {
      case PackageBox(_, Package(_, _, MessageBox(_, Container(mbox)))) => mbox
      case PackageBox(_, Package(_, _, mb @ MessageBox(_, _))) => Seq(mb)
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
