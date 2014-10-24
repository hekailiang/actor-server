package org.specs2.mutable

import akka.actor._
import akka.io.Tcp.{Close, Received, Write}
import akka.testkit.{ TestKitBase, TestProbe }
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.websudos.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.api.frontend.ws.WSFrontend
import com.secretapp.backend.api.frontend.{JsonConnection, MTConnection, TransportConnection}
import com.secretapp.backend.api.frontend.tcp.TcpFrontend
import com.secretapp.backend.api.{ ClusterProxies, Singletons }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc.{Request, RpcRequestMessage, RpcResponse}
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
import org.specs2.execute.StandardResults
import org.specs2.matcher._
import org.specs2.specification.{Fragments, Fragment}
import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.language.{ implicitConversions, postfixOps }
import scala.util.Random
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import java.net.InetSocketAddress

trait ActorServiceImplicits {
  def codecRes2BS(res: String \/ BitVector): ByteString = ByteString(res.toOption.get.toByteBuffer)
  implicit def eitherBitVector2ByteString(v: String \/ BitVector): ByteString = codecRes2BS(v)

  implicit def byteVector2ByteString(v: ByteVector): ByteString = ByteString(v.toByteBuffer)
  implicit def byteString2ByteVector(v: ByteString): ByteVector = ByteVector(v.toByteBuffer)
  implicit def bitVector2ByteString(v: BitVector): ByteString = ByteString(v.toByteBuffer)
  implicit def bitString2BitVector(v: ByteString): BitVector = BitVector(v.toByteBuffer)
}

trait ActorCommon { self: RandomService =>
  implicit val csession: CSession

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong)
  }

  case class TestScopeNew(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, authId: Long, userOpt: Option[User] = None) {
    lazy val user = userOpt.get
  }
}

trait ActorReceiveHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  this: ActorLikeSpecification =>

  private var packageIndex = 0
  private var messageId = 0

  val defaultTimeout: FiniteDuration = 10.seconds

  def transportForeach(f: (TransportConnection) => Fragments) = {
    Seq(MTConnection, JsonConnection) foreach { t =>
      addFragments(t.toString, f(t), "session")
    }
    success
  }

  def sendMsg(data: ByteString)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = transport match {
    case MTConnection => scope.probe.send(scope.apiActor, Received(data))
    case JsonConnection => scope.probe.send(scope.apiActor, TextFrame(data))
  }

  def sendMsgBox(msg: MessageBox)(implicit scope: TestScopeNew, transport: TransportConnection) = {
    sendMsgBoxes(Set(msg))
  }

  def sendMsg(msg: TransportMessage)(implicit scope: TestScopeNew, transport: TransportConnection) = {
    sendMsgs(Set(msg))
  }

  def genMessageId() = {
    val id = messageId
    messageId += 4
    id
  }

  def sendMsgs(msgs: immutable.Set[TransportMessage])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    val mboxes = msgs map (MessageBox(genMessageId, _))
    sendMsgBoxes(mboxes.toSet)
  }

  def serializeMsg(msg: TransportMessage)(implicit scope: TestScopeNew, transport: TransportConnection) = {
    val mb = MessageBox(genMessageId, msg)
    transport match {
      case MTConnection =>
        val p = protoPackageBox.build(packageIndex, scope.authId, scope.session.id, mb)
        codecRes2BS(p)
      case JsonConnection =>
        val p = JsonPackage.build(scope.authId, scope.session.id, mb)
        p.encode
    }
  }

  def sendMsgBoxes(msgs: immutable.Set[MessageBox])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    msgs foreach { msg =>
      transport match {
        case MTConnection =>
          val p = protoPackageBox.build(packageIndex, scope.authId, scope.session.id, msg)
          scope.probe.send(scope.apiActor, Received(codecRes2BS(p)))
        case JsonConnection =>
          val p = JsonPackage.build(scope.authId, scope.session.id, msg)
          scope.probe.send(scope.apiActor, TextFrame(p.encode))
      }
      packageIndex += 1
    }
  }

  def sendRpcMsg(msg: RpcRequestMessage)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    sendRpcMsgs(Set(msg))
  }

  def sendRpcMsgs(msgs: immutable.Set[RpcRequestMessage])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    sendMsgs(msgs map (m => RpcRequestBox(Request(m))))
  }

  def sendMessageAck(msgIds: immutable.Set[Long])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    sendMsg(MessageAck(msgIds.toVector))
  }

  def expectMsg(msg: TransportMessage, withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    expectMsgs(Set(msg), withNewSession, duration)
  }

  def expectMsgByPF(withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[TransportMessage, Unit])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    def f(acks: immutable.Set[Long], receivedMsgByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = {
      def g(msgs: Seq[TransportMessage], acks: immutable.Set[Long], receivedMsgByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = msgs match {
        case m :: msgs =>
          Try(pf(m)) match {
            case Success(_) => g(msgs, acks.toSet, receivedMsgByPF = true, receivedNewSession = receivedNewSession)
            case Failure(_) => m match {
              case MessageAck(acks) => g(msgs, acks.toSet, receivedMsgByPF, receivedNewSession)
              case NewSession(_, sesId) if withNewSession && !receivedNewSession && sesId == scope.session.id =>
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
    sendMessageAck(f(Set(), false, false))
  }

  def expectMsgsWhileByPF(withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[TransportMessage, Boolean])(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    def f(acks: immutable.Set[Long], receivedByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = {
      def g(msgs: Seq[TransportMessage], acks: immutable.Set[Long], receivedByPF: Boolean, receivedNewSession: Boolean): immutable.Set[Long] = msgs match {
        case m :: msgs =>
          Try(pf(m)) match {
            case Success(res) => g(msgs, acks.toSet, receivedByPF = !res, receivedNewSession = receivedNewSession)
            case Failure(_) => m match {
              case MessageAck(acks) => g(msgs, acks.toSet, receivedByPF, receivedNewSession)
              case NewSession(_, sesId) if withNewSession && !receivedNewSession && sesId == scope.session.id =>
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
    sendMessageAck(f(Set(), false, false))
  }

  def expectMsgs(msgs: immutable.Set[TransportMessage], withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    def f(messages: immutable.Set[TransportMessage], acks: immutable.Set[Long]): immutable.Set[Long] = {
      def g(data: ByteString) = {
        val receivedMsgs = mutable.Set[TransportMessage]()
        val receivedAcks = mutable.Set[Long]()
        val msgBoxes = deserializeMsgBoxes(deserializePackage(data))
        msgBoxes foreach {
          case MessageBox(msgId, msg) => msg match {
            case MessageAck(acks) =>
              receivedAcks ++= acks
            case NewSession(_, sesId) if withNewSession && sesId == scope.session.id =>
              val newSessionMsgOpt = messages find {
                case NewSession(0, scope.session.id) => true
                case _ => false
              }
              newSessionMsgOpt match {
                case Some(m) => receivedMsgs += m
                case _ => throw new IllegalArgumentException(s"NewSession has not received")
              }
            case m =>
              if (messages.contains(m)) receivedMsgs += m
              else throw new IllegalArgumentException(s"unknown message: $m, expect: $messages")
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
    val acks =
      if (withNewSession) f(msgs ++ Set(NewSession(0, scope.session.id)), Set())
      else f(msgs, Set())
    sendMessageAck(acks)
  }

  def expectRpcMsg(msg: RpcResponse, withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    expectRpcMsgs(Set(msg), withNewSession, duration)
  }

  def expectRpcMsgs(msgs: immutable.Set[RpcResponse], withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew, transport: TransportConnection): Unit = {
    var receivedNewSession = false
    def f(messages: immutable.Set[RpcResponse], acks: immutable.Set[Long]): immutable.Set[Long] = {
      def g(data: ByteString) = {
        val receivedMsgs = mutable.Set[RpcResponse]()
        val receivedAcks = mutable.Set[Long]()
        val msgBoxes = deserializeMsgBoxes(deserializePackage(data))
        msgBoxes foreach {
          case MessageBox(msgId, msg) => msg match {
            case MessageAck(acks) =>
              receivedAcks ++= acks
            case NewSession(_, sesId) if withNewSession && sesId == scope.session.id && !receivedNewSession =>
              receivedNewSession = true
            case RpcResponseBox(_, rpcMsg) =>
              if (messages.contains(rpcMsg)) receivedMsgs += rpcMsg
              else throw new IllegalArgumentException(s"unknown rpc message: $rpcMsg, expect: $messages")
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
    sendMessageAck(f(msgs, Set()))
  }

  private def deserializePackage(data: ByteString)(implicit scope: TestScopeNew, transport: TransportConnection) = {
    val p = transport match {
      case MTConnection => MTPackageBoxCodec.decodeValidValue(data).p
      case JsonConnection => JsonPackageCodec.decode(data).toOption.get
    }
    if (p.authId != scope.authId || p.sessionId != scope.session.id)
      throw new IllegalArgumentException(s"p.authId(${p.authId}}) != authId(${scope.authId}) || p.sessionId(${p.sessionId}) != s.id(${scope.session.id})")
    p
  }

  private def deserializeMsgBoxes(p: TransportPackage): Seq[MessageBox] = {
    p.decodeMessageBox.toOption.get match {
      case MessageBox(_, Container(mboxes)) => mboxes
      case mb@MessageBox(_, _) => Seq(mb)
    }
  }

  @tailrec
  private def receiveOne[A](f: (ByteString) => A, e: () => A)(duration: Duration)(implicit scope: TestScopeNew): A = {
    scope.probe.receiveOne(duration) match {
      case Write(data, _) => f(data)
      case FrameCommand(frame: TextFrame) => f(frame.payload)
      case FrameCommand(_: CloseFrame) | Close => receiveOne(f, e)(duration)
      case null => e()
      case msg => throw new Exception(s"Unknown msg: $msg")
    }
  }
}

trait ActorServiceHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  self: TestKitBase with StandardResults with ShouldExpectations with AnyMatchers with TraversableMatchers =>

  Security.addProvider(new BouncyCastleProvider())

  val mockAuthId = rand.nextLong()
  val defaultPhoneNumber = 79853867016L

  protected var incMessageId = 0L

  val counters = new Singletons
  val clusterProxies = new ClusterProxies
  val sessionRegion = SessionActor.startRegion()(system, counters, clusterProxies, csession)

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
    AuthIdRecord.insertEntity(AuthId(authId, userId)).sync()
  }

  def addUser(authId: Long, sessionId: Long, u: User, phoneNumber: Long): Unit = blocking {
    AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
  }

  def authUser(u: User, phoneNumber: Long): User = blocking {
    insertAuthId(u.authId, u.uid.some)
    UserRecord.insertEntityWithPhoneAndPK(u).sync()
    u
  }

  def authDefaultUser(uid: Int, phoneNumber: Long = defaultPhoneNumber)(implicit destActor: ActorRef, s: SessionIdentifier, authId: Long): User = blocking {
    val publicKey = hex"ac1d".bits
    val name = s"Timothy${uid} Klim${uid}"
    val user = User.build(uid = uid, authId = authId, publicKey = publicKey, accessSalt = "salt",
      phoneNumber = phoneNumber, name = name)
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

  def genTestScope()(implicit transport: TransportConnection) = {
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
    val userStruct = User.build(uid = userId, authId = scope.authId, publicKey = publicKey, accessSalt = salt,
      phoneNumber = phoneNumber, name = name)
    val user = authUser(userStruct, phoneNumber)
    scope.copy(userOpt = user.some)
  }

  def getProbeAndActor()(implicit transport: TransportConnection) = {
    val probe = TestProbe()
    val actor = transport match {
      case MTConnection => system.actorOf(TcpFrontend.props(probe.ref, inetAddr, sessionRegion, csession))
      case JsonConnection =>
        val props = Props(new WSFrontend(probe.ref, inetAddr, sessionRegion, csession) {
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
      val newUser = User.build(
        uid = uid, authId = authId, publicKey = BitVector.fromLong(rand.nextLong), accessSalt = "salt",
        phoneNumber = phone, name = s"Timothy${uid} Klim${uid}"
      )
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
          println(s"unknown receive ${x}")
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
          MessageBoxCodec.encodeValid(MessageBox(getMessageId(), MessageAck(Vector(mb.messageId))))
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
    }.fold(0) {_ + _}
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
