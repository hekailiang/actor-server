package org.specs2.mutable

import akka.actor.ActorRef
import akka.io.Tcp.{Write, Received}
import akka.testkit.{TestProbe, TestKitBase}
import akka.util.ByteString
import com.secretapp.backend.data.transport.{Package, PackageBox, MessageBox}
import com.secretapp.backend.services.common.PackageCommon.Authenticate
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import scala.concurrent.blocking
import scala.language.implicitConversions
import com.secretapp.backend.data.message.{Container, MessageAck, TransportMessage, NewSession}
import com.secretapp.backend.data.models.{Phone, SessionId, AuthId, User}
import com.secretapp.backend.persist.{PhoneRecord, UserRecord, SessionIdRecord, AuthIdRecord}
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.services.common.RandomService
import scodec.bits._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.util.testing.AsyncAssertionsHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicLong
import java.security.{Security, KeyPairGenerator, SecureRandom}

trait ActorServiceHelpers extends RandomService {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers =>

  Security.addProvider(new BouncyCastleProvider())

  val mockAuthId = rand.nextLong()
  val defaultPhoneNumber = 79853867016L

  implicit val session: CSession

  protected val incMessageId = new AtomicLong(1L)

  def codecRes2BS(res: String \/ BitVector): ByteString = {
    ByteString(res.toOption.get.toByteBuffer)
  }

  def newSession(sessionId: Long, messageId: Long) = {
    protoPackageBox.build(mockAuthId, sessionId, messageId, NewSession(sessionId, messageId))
  }

  def genPublicKey = {
    val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
    val g = KeyPairGenerator.getInstance("ECDSA", "BC")
    g.initialize(ecSpec, new SecureRandom())
    val pair = g.generateKeyPair()
    val pubKey = pair.getPublic.asInstanceOf[ECPublicKey]
    BitVector(pubKey.getQ.getEncoded)
  }

  def insertAuthId(userId: Option[Int] = None): Unit = blocking {
    AuthIdRecord.insertEntity(AuthId(mockAuthId, userId)).sync()
  }

  def insertSessionId(implicit s: SessionIdentifier): Unit = blocking {
    SessionIdRecord.insertEntity(SessionId(mockAuthId, s.id)).sync()
  }

  def insertAuthAndSessionId(userId: Option[Int] = None)(implicit s: SessionIdentifier): Unit = {
    insertAuthId(userId)
    insertSessionId
  }

  def addUser(authId: Long, sessionId: Long, u: User, phoneNumber: Long): Unit = blocking {
    AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
    SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
  }

  def authUser(u: User, phoneNumber: Long)(implicit destActor: ActorRef, s: SessionIdentifier): User = blocking {
    insertAuthAndSessionId(u.uid.some)
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
    destActor ! Authenticate(u)
    u
  }

  def authDefaultUser(uid: Int = 1, phoneNumber: Long = defaultPhoneNumber)(implicit destActor: ActorRef, s: SessionIdentifier): User = blocking {
    val publicKey = hex"ac1d".bits
    val name = s"Timothy${uid} Klim${uid}"
    val user = User.build(uid = uid, authId = mockAuthId, publicKey = publicKey, accessSalt = "salt",
      phoneNumber = phoneNumber, name = name)
    val accessHash = User.getAccessHash(mockAuthId, uid, "salt")
    authUser(user, phoneNumber)
  }

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong)
  }

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  case class WrappedReceivePackage(f: (Long) => Unit)

  implicit class WrappedTM(m: TransportMessage) {
    def :~>(wp: WrappedReceivePackage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): Unit = {
      val r = pack(m)
      probe.send(destActor, Received(r.blob))
      wp.f(r.messageId)
    }
  }

  def @<~:(m: TransportMessage)(implicit probe: TestProbe, s: SessionIdentifier): WrappedReceivePackage = WrappedReceivePackage { (messageId: Long) =>
    val res = pack(m, messageId)
    val ack = pack(MessageAck(Array(messageId)), messageId) // TODO: move into DSL method
    val ses = pack(NewSession(s.id, messageId), messageId) // TODO: move into DSL method
    val expectedMsg = Seq(ses, ack, res)
    probe.expectMsgAllOf(expectedMsg.map(m => Write(m.blob)) :_*)
  }

  case class PackResult(blob: ByteString, messageId: Long)

  def pack(m: MessageBox)(implicit s: SessionIdentifier): PackResult = {
    pack(m.body, m.messageId)
  }

  def pack(m: TransportMessage, messageId: Long = incMessageId.incrementAndGet())
          (implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(mockAuthId, s.id, messageId, m)
    PackResult(codecRes2BS(p), messageId)
  }

  def ack(messageId: Long)(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(mockAuthId, s.id, messageId, MessageAck(Array(messageId)))
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
