package org.specs2.mutable

import akka.actor.ActorRef
import akka.io.Tcp.{Write, Received}
import akka.testkit.{TestProbe, TestKitBase}
import akka.util.ByteString
import com.secretapp.backend.data.transport.{Package, PackageBox, MessageBox}
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import scala.concurrent.blocking
import scala.language.implicitConversions
import com.secretapp.backend.data.message.{Container, MessageAck, TransportMessage, NewSession}
import com.secretapp.backend.data.models.{SessionId, AuthId}
import com.secretapp.backend.persist.{SessionIdRecord, AuthIdRecord}
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.services.common.RandomService
import scodec.bits._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.util.testing.AsyncAssertionsHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicLong

trait ActorServiceHelpers extends RandomService {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers =>

  val mockAuthId = rand.nextLong()

  implicit val session: CSession

  protected val incMessageId = new AtomicLong(1L)

  def codecRes2BS(res: String \/ BitVector): ByteString = {
    ByteString(res.toOption.get.toByteBuffer)
  }

  def newSession(sessionId: Long, messageId: Long) = {
    protoPackageBox.build(mockAuthId, sessionId, messageId, NewSession(sessionId, messageId))
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

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong)
  }

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  case class WrappedReceivePackage(f: (Long) => Unit)

  implicit class WrappedTM(m: TransportMessage) {
    def :~>(wp: WrappedReceivePackage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier) = {
      val r = pack(m)
      probe.send(destActor, Received(r.blob))
      wp.f(r.messageId)
      success
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

  def expectMsgWithAck(messages: MessageBox*)(implicit probe: TestProbe) = {
    val receivedPackages = probe.receiveN(messages.size * 2)
    val unboxed = for ( p <- receivedPackages ) yield {
      p match {
        case Write(bs, _) => protoPackageBox.decode(bs).toOption.get._2
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
