package org.specs2.mutable

import akka.actor._
import akka.io.Tcp.{Write, Received}
import akka.testkit.{TestProbe, TestKitBase}
import akka.util.ByteString
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.Counters
import com.secretapp.backend.api.CountersProxies
import com.secretapp.backend.data.transport.{Package, PackageBox, MessageBox}
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon.Authenticate
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.scalamock.specs2.MockFactory
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import scala.concurrent.blocking
import scala.language.implicitConversions
import com.secretapp.backend.data.message.{Container, MessageAck, TransportMessage, NewSession}
import com.secretapp.backend.data.models.{Phone, SessionId, AuthId, User}
import com.secretapp.backend.persist.{PhoneRecord, UserRecord, SessionIdRecord, AuthIdRecord}
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.services.common.RandomService
import scala.util.Random
import scodec.bits._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.util.testing.AsyncAssertionsHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicLong
import java.security.{Security, KeyPairGenerator, SecureRandom}

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

  val counters = new Counters
  val countersProxies = new CountersProxies

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, countersProxies)(session) with RandomServiceMock with GeneratorServiceMock))
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
      val r = pack(authId, m)
      probe.send(destActor, Received(r.blob))
      wp.f(r.messageId)
    }
  }

  def @<~:(m: TransportMessage)(implicit probe: TestProbe, s: SessionIdentifier, authId: Long): WrappedReceivePackage = WrappedReceivePackage { (messageId: Long) =>
    val res = pack(authId, m, messageId)
    val ack = pack(authId, MessageAck(Array(messageId)), messageId) // TODO: move into DSL method
    val ses = pack(authId, NewSession(s.id, messageId), messageId) // TODO: move into DSL method
    val expectedMsg = Seq(ses, ack, res)
    probe.expectMsgAllOf(expectedMsg.map(m => Write(m.blob)) :_*)
  }

  case class PackResult(blob: ByteString, messageId: Long)

  def pack(authId: Long, m: MessageBox)(implicit s: SessionIdentifier): PackResult = {
    pack(authId, m.body, m.messageId)
  }

  def pack(authId: Long, m: TransportMessage, messageId: Long = incMessageId.incrementAndGet())
          (implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(authId, s.id, messageId, m)
    PackResult(codecRes2BS(p), messageId)
  }

  def ack(messageId: Long, authId: Long)(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(authId, s.id, messageId, MessageAck(Array(messageId)))
    PackResult(codecRes2BS(p), messageId)
  }

  def receiveNWithAck(n: Int)(implicit probe: TestProbe): Seq[MessageBox] = {
    val receivedPackages = probe.receiveN(n * 2)
    val unboxed = for ( p <- receivedPackages ) yield {
      p match {
        case Write(bs, _) =>
          protoPackageBox.decode(bs) match {
            case -\/(e) =>
              throw new Exception(s"Cannot decode PackageBox $e")
            case \/-(p) =>
              p._2
          }
      }
    }
    val mboxes = unboxed flatMap {
      case PackageBox(Package(_, _, MessageBox(_, Container(mbox)))) => mbox
      case PackageBox(Package(_, _, mb@MessageBox(_, _))) => Seq(mb)
    }
    val (ackPackages, packages) = mboxes partition {
      case MessageBox(_, ma@MessageAck(_)) => true
      case _ => false
    }
    ackPackages.length should equalTo(packages.length)
    packages
  }

  def receiveOneWithAck()(implicit probe: TestProbe): MessageBox = {
    receiveNWithAck(1) match {
      case Seq(x) => x
      case Seq() => null
      case _ => throw new Exception("Received more than one message")
    }
  }

  def expectMsgWithAck(messages: MessageBox*)(implicit probe: TestProbe) = {
    val receivedPackages = probe.receiveN(messages.size * 2)
    val unboxed = for ( p <- receivedPackages ) yield {
      p match {
        case Write(bs, _) =>
          protoPackageBox.decode(bs) match {
            case -\/(e) =>
              throw new Exception(s"Cannot decode PackageBox $e")
            case \/-(p) =>
              p._2
          }
      }
    }
    val mboxes = unboxed flatMap {
      case PackageBox(Package(_, _, MessageBox(_, Container(mbox)))) => mbox
      case PackageBox(Package(_, _, mb@MessageBox(_, _))) => Seq(mb)
    }
    val (ackPackages, expectedPackages) = mboxes partition {
      case MessageBox(_, ma@MessageAck(_)) => true
      case _ => false
    }
    val acks = ackPackages flatMap(a => a.body.asInstanceOf[MessageAck].messageIds)
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
