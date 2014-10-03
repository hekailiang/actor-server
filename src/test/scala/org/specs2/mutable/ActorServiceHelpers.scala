package org.specs2.mutable

import akka.actor._
import akka.io.Tcp.{Close, Received, Write}
import akka.testkit.{ TestKitBase, TestProbe }
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.api.frontend.ws.WSFrontend
import com.secretapp.backend.api.frontend.{JsonConnection, MTConnection, TransportConnection}
import com.secretapp.backend.api.frontend.tcp.TcpFrontend
import com.secretapp.backend.api.{ ClusterProxies, Singletons }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist.{ AuthIdRecord, UserRecord }
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.transport.{JsonPackageCodec, MTPackageBoxCodec}
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.session._
import spray.can.websocket.frame._
import spray.can.websocket._
import java.security.{ KeyPairGenerator, SecureRandom, Security }
import java.util.concurrent.atomic.AtomicLong
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.scalamock.specs2.MockFactory
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import org.specs2.specification.{Fragments, Fragment}
import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.language.implicitConversions
import scala.util.Random
import scalaz._
import scalaz.Scalaz._
import scodec.bits._

trait ActorServiceImplicits {
  def codecRes2BS(res: String \/ BitVector): ByteString = ByteString(res.toOption.get.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2ByteVector(v: ByteString): ByteVector = ByteVector(v.toByteBuffer)
  implicit def bitVector2ByteString(v: BitVector): ByteString = ByteString(v.toByteBuffer)
  implicit def bitString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
}

trait ActorCommon { self: RandomService =>
  implicit val session: CSession

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong)
  }
}

trait ActorReceiveHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  this: ActorLikeSpecification =>

  private var packageIndex = 0
  private var messageId = 0

  def transportForeach(f: (TransportConnection) => Fragments) = {
    Seq(MTConnection, JsonConnection) foreach { t =>
      addFragments(t.toString, f(t), "session")
    }
    success
  }

  def sendMsg(data: ByteString)(implicit probe: TestProbe, destActor: ActorRef, transport: TransportConnection): Unit = transport match {
    case MTConnection => probe.send(destActor, Received(data))
    case JsonConnection => probe.send(destActor, TextFrame(data))
  }

  def sendMsgBox(msg: MessageBox)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long) = {
    sendMsgBoxes(Set(msg))
  }

  def sendMsg(msg: TransportMessage)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long) = {
    sendMsgs(Set(msg))
  }

  def sendMsgs(msgs: immutable.Set[TransportMessage])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): Unit = {
    val mboxes = msgs map { m =>
      val mbox = MessageBox(messageId, m)
      messageId += 4
      mbox
    }
    sendMsgBoxes(mboxes.toSet)
  }

  def sendMsgBoxes(msgs: immutable.Set[MessageBox])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): Unit = {
    msgs foreach { msg =>
      transport match {
        case MTConnection =>
          val p = protoPackageBox.build(packageIndex, authId, s.id, msg)
          probe.send(destActor, Received(codecRes2BS(p)))
        case JsonConnection =>
          val p = JsonPackage.build(authId, s.id, msg)
          probe.send(destActor, TextFrame(p.encode))
      }
      packageIndex += 1
    }
  }

  def expectMsg(msg: TransportMessage, withNewSession: Boolean = false, duration: Duration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): Unit = {
    expectMsgs(Set(msg), withNewSession, duration)
  }

  def expectMsgByPF(withNewSession: Boolean = false, duration: Duration = 3.seconds)(pf: PartialFunction[TransportMessage, Unit])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): immutable.Set[Long] = {
    def f(acks: immutable.Set[Long], receivedMsgByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = {
      def g(msgs: Seq[TransportMessage], acks: immutable.Set[Long], receivedMsgByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = msgs match {
        case m :: msgs =>
          Try(pf(m)) match {
            case Success(_) => g(msgs, acks.toSet, receivedMsgByPF = true, receivedNewSession = receivedNewSession)
            case Failure(_) => m match {
              case MessageAck(acks) => g(msgs, acks.toSet, receivedMsgByPF, receivedNewSession)
              case NewSession(_, sesId) if withNewSession && !receivedNewSession && sesId == s.id =>
                g(msgs, acks, receivedMsgByPF, receivedNewSession = true)
            }
          }
        case Nil =>
          if (receivedMsgByPF && (!withNewSession || (withNewSession && receivedNewSession))) acks
          else f(acks, receivedMsgByPF, receivedNewSession)
      }

      receiveOne({ data =>
        val msgs = deserializeMsgBoxes(deserializePackage(data)).map(_.body)
        g(msgs, acks, receivedMsgByPF, receivedNewSession)
      }, { () =>
        throw new IllegalArgumentException(s"no messages")
      })(duration)
    }
    f(Set(), false, false)
  }

  def expectMsgsWhileByPF(withNewSession: Boolean = false, duration: Duration = 3.seconds)(pf: PartialFunction[TransportMessage, Boolean])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): immutable.Set[Long] = {
    def f(acks: immutable.Set[Long], receivedByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = {
      def g(msgs: Seq[TransportMessage], acks: immutable.Set[Long], receivedByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = msgs match {
        case m :: msgs =>
          Try(pf(m)) match {
            case Success(res) => g(msgs, acks.toSet, receivedByPF = !res, receivedNewSession = receivedNewSession)
            case Failure(_) => m match {
              case MessageAck(acks) => g(msgs, acks.toSet, receivedByPF, receivedNewSession)
              case NewSession(_, sesId) if withNewSession && !receivedNewSession && sesId == s.id =>
                g(msgs, acks, receivedByPF, receivedNewSession = true)
            }
          }
        case Nil =>
          if (receivedByPF && (!withNewSession || (withNewSession && receivedNewSession))) acks
          else f(acks, receivedByPF, receivedNewSession)
      }

      receiveOne({ data =>
        val msgs = deserializeMsgBoxes(deserializePackage(data)).map(_.body)
        g(msgs, acks, receivedByPF, receivedNewSession)
      }, { () =>
        throw new IllegalArgumentException(s"no messages")
      })(duration)
    }
    f(Set(), false, false)
  }

  def expectMsgs(msgs: immutable.Set[TransportMessage], withNewSession: Boolean = false, duration: Duration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, transport: TransportConnection, authId: Long): Unit = {
    def f(messages: immutable.Set[TransportMessage], acks: immutable.Set[Long]): immutable.Set[Long] = {
      def g(data: ByteString) = {
        val receivedMsgs = mutable.Set[TransportMessage]()
        val receivedAcks = mutable.Set[Long]()
        val msgBoxes = deserializeMsgBoxes(deserializePackage(data))
        msgBoxes foreach {
          case MessageBox(msgId, msg) => msg match {
            case MessageAck(acks) =>
              receivedAcks ++= acks
            case NewSession(_, sesId) if withNewSession && sesId == s.id =>
              val m = messages.filter { case NewSession(0, s.id) => true }.head
              receivedMsgs += m
            case m =>
              if (messages.contains(m)) receivedMsgs += m
              else throw new IllegalArgumentException(s"unknown message: $m")
          }
        }
        (messages -- receivedMsgs, acks ++ receivedAcks)
      }

      receiveOne({ data =>
        val (remain, acks) = g(data)
        if (!remain.isEmpty) f(remain, acks)
        else acks
      }, { () =>
        throw new IllegalArgumentException(s"unreceived messages: $messages")
      })(duration)
    }
    if (withNewSession) f(msgs ++ Set(NewSession(0, s.id)), Set())
    else f(msgs, Set())
  }

  private def deserializePackage(data: ByteString)(implicit s: SessionIdentifier, transport: TransportConnection, authId: Long) = {
    val p = transport match {
      case MTConnection => MTPackageBoxCodec.decodeValidValue(data).p
      case JsonConnection => JsonPackageCodec.decode(data).toOption.get
    }
    if (p.authId != authId || p.sessionId != s.id)
      throw new IllegalArgumentException(s"p.authId(${p.authId}}) != authId($authId) || p.sessionId(${p.sessionId}) != s.id(${s.id})")
    p
  }

  private def deserializeMsgBoxes(p: TransportPackage) = {
    p.decodeMessageBox.toOption.get match {
      case MessageBox(_, Container(mboxes)) => mboxes
      case mb@MessageBox(_, _) => Seq(mb)
    }
  }

  @tailrec
  private def receiveOne[A](f: (ByteString) => A, e: () => A)(duration: Duration)(implicit probe: TestProbe): A = {
    probe.receiveOne(duration) match {
      case Write(data, _) => f(data)
      case FrameCommand(frame: TextFrame) => f(frame.payload)
      case FrameCommand(_: CloseFrame) | Close => receiveOne(f, e)(duration)
      case null => e()
      case msg => throw new Exception(s"Unknown msg: $msg")
    }
  }
}

trait ActorServiceHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers with MockFactory =>

  Security.addProvider(new BouncyCastleProvider())

  val mockAuthId = rand.nextLong()
  val defaultPhoneNumber = 79853867016L

  protected val incMessageId = new AtomicLong(1L)

  val counters = new Singletons
  implicit val clusterProxies = new ClusterProxies
  val sessionRegion = SessionActor.startRegion()

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

  def addUser(authId: Long, sessionId: Long, u: User, phoneNumber: Long): Unit = blocking {
    AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
  }

  def authUser(u: User, phoneNumber: Long)(implicit destActor: ActorRef, s: SessionIdentifier): User = blocking {
    insertAuthId(u.authId, u.uid.some)
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
    u
  }

  def authDefaultUser(uid: Int, phoneNumber: Long = defaultPhoneNumber)(implicit destActor: ActorRef, s: SessionIdentifier, authId: Long): User = blocking {
    val publicKey = hex"ac1d".bits
    val name = s"Timothy${uid} Klim${uid}"
    val user = User.build(uid = uid, authId = authId, publicKey = publicKey, accessSalt = "salt",
      phoneNumber = phoneNumber, name = name)
    val accessHash = User.getAccessHash(authId, uid, "salt")
    authUser(user, phoneNumber)
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

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new TcpFrontend(probe.ref, sessionRegion, session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

  def getProbeAndActor()(implicit transport: TransportConnection) = {
    val probe = TestProbe()
    val actor = transport match {
      case MTConnection => system.actorOf(TcpFrontend.props(probe.ref, sessionRegion, session))
      case JsonConnection =>
        val props = Props(new WSFrontend(probe.ref, sessionRegion, session) {
          override def receive = businessLogic orElse closeLogic
        })
        system.actorOf(props)
    }
    (probe, actor)
  }

  case class TestScope(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, user: User) {
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

    def apply(uid: Int, phone: Long): TestScope = {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      implicit val authId = rand.nextLong
      val user = authDefaultUser(uid, phone)
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

  def tcpReceiveN(n: Int, duration: FiniteDuration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef) = {
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
        case x =>
          println(s"unknown receive ${x}")
          receive(acc)
      }

    }

    receive()
  }

  def protoReceiveN(n: Int, duration: FiniteDuration = 3.seconds)(implicit probe: TestProbe, destActor: ActorRef): immutable.Seq[MTPackage] = {
    tcpReceiveN(n, duration) map {
      case Write(data, _) =>
        val p = MTPackageBoxCodec.decodeValidValue(data).p
        // FIXME: real index
        val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)
        val pb = MTPackageBox(0, MTPackage(p.authId, p.sessionId,
          MessageBoxCodec.encodeValid(
            MessageBox(mb.messageId * 10, MessageAck(Vector(mb.messageId)))
          )
        ))
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

  def pack(index: Int, authId: Long, m: TransportMessage, messageId: Long = incMessageId.incrementAndGet())(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, m)
    PackResult(codecRes2BS(p), messageId)
  }

  def ack(index: Int, messageId: Long, authId: Long)(implicit s: SessionIdentifier): PackResult = {
    val p = protoPackageBox.build(index, authId, s.id, messageId, MessageAck(Vector(messageId)))
    PackResult(codecRes2BS(p), messageId)
  }

  def receiveNWithAck(n: Int)(implicit probe: TestProbe, destActor: ActorRef): Seq[MessageBox] = {
    val receivedPackages = protoReceiveN(n)

    val mboxes = receivedPackages flatMap {
      case MTPackage(authId, sessionId, mboxBytes) =>
        val messageBox = MessageBoxCodec.decodeValue(mboxBytes).toOption.get

        messageBox match {
          case MessageBox(_, Container(mbox)) => mbox
          case mb @ MessageBox(_, _) => Seq(mb)
        }
    }
    val (ackPackages, packages) = mboxes partition {
      case MessageBox(_, ma @ MessageAck(_)) => true
      case _ => false
    }

    packages
  }

  def receiveOneWithAck()(implicit probe: TestProbe, destActor: ActorRef): MessageBox = {
    receiveNWithAck(2) match {
      case Seq(x) => x
      case Seq() => null
      case _ => throw new Exception("Received more than one message")
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
