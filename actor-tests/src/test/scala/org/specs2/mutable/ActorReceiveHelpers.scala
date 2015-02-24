package org.specs2.mutable

import akka.io.Tcp.{ Close, Received, Write }
import akka.util.ByteString
import com.secretapp.backend.api.frontend.{ MTConnection, TransportConnection }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.transport.MTPackageBoxCodec
import com.secretapp.backend.services.common.RandomService
import org.specs2.specification.Fragments
import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scalaz.Scalaz._
import spray.can.websocket.FrameCommand
import spray.can.websocket.frame._

trait ActorReceiveHelpers extends RandomService with ActorServiceImplicits with ActorCommon {
  this: ActorLikeSpecification =>

  private var packageIndex = 0
  private var messageId = 0

  val defaultTimeout: FiniteDuration = 10.seconds

  def transportForeach(f: (TransportConnection) => Fragments) = {
    Seq(MTConnection) foreach { t =>
      addFragments(t.toString, f(t), "session")
    }
    success
  }

  def sendMsg(data: ByteString)(implicit scope: TestScopeNew): Unit =
    scope.probe.send(scope.apiActor, Received(data))

  def sendMsgBox(msg: MessageBox)(implicit scope: TestScopeNew) = {
    sendMsgBoxes(Set(msg))
  }

  def sendMsg(msg: TransportMessage)(implicit scope: TestScopeNew) = {
    sendMsgs(Set(msg))
  }

  def genMessageId() = {
    val id = messageId
    messageId += 4
    id
  }

  def sendMsgs(msgs: immutable.Set[TransportMessage])(implicit scope: TestScopeNew): Unit = {
    val mboxes = msgs map (MessageBox(genMessageId, _))
    sendMsgBoxes(mboxes.toSet)
  }

  def serializeMsg(msg: TransportMessage)(implicit scope: TestScopeNew) = {
    val mb = MessageBox(genMessageId, msg)
    val p = protoPackageBox.build(packageIndex, scope.authId, scope.session.id, mb)
    codecRes2BS(p)
  }

  def sendMsgBoxes(msgs: immutable.Set[MessageBox])(implicit scope: TestScopeNew): Unit = {
    msgs foreach { msg =>
      val p = protoPackageBox.build(packageIndex, scope.authId, scope.session.id, msg)
      scope.probe.send(scope.apiActor, Received(codecRes2BS(p)))
      packageIndex += 1
    }
  }

  def sendRpcMsg(msg: RpcRequestMessage)(implicit scope: TestScopeNew): Unit = {
    sendRpcMsgs(Set(msg))
  }

  def sendRpcMsgs(msgs: immutable.Set[RpcRequestMessage])(implicit scope: TestScopeNew): Unit = {
    sendMsgs(msgs map (m => RpcRequestBox(Request(m))))
  }

  def sendMessageAck(msgIds: immutable.Set[Long])(implicit scope: TestScopeNew): Unit = {
    sendMsg(MessageAck(msgIds.toVector))
  }

  def expectMsg(msg: TransportMessage, withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew): Unit = {
    expectMsgs(Set(msg), withNewSession, duration)
  }

  def expectRpcMsgByPF[T](withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[RpcResponseMessage, T])(implicit scope: TestScopeNew): T = {
    expectMsgByPF(withNewSession, duration) {
      case RpcResponseBox(_, Ok(res)) => pf(res)
    }
  }

  def expectRpcByPF[T](withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[RpcResponse, T])(implicit scope: TestScopeNew): T = {
    expectMsgByPF(withNewSession, duration) {
      case RpcResponseBox(_, res) => pf(res)
    }
  }

  def expectMsgByPF[T](withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[TransportMessage, T])(implicit scope: TestScopeNew): T = {
    def f(acks: immutable.Set[Long], obj: Option[T], receivedNewSession: Boolean): (T, immutable.Set[Long]) = {
      def g(msgs: Seq[TransportMessage], acks: immutable.Set[Long], obj: Option[T], receivedNewSession: Boolean): (T, immutable.Set[Long]) = msgs match {
        case m :: msgs =>
          Try(pf(m)) match {
            case Success(obj) => g(msgs, acks.toSet, obj.some, receivedNewSession = receivedNewSession)
            case Failure(_) => m match {
              case MessageAck(acks) => g(msgs, acks.toSet, obj, receivedNewSession)
              case NewSession(_, sesId) if withNewSession && !receivedNewSession && sesId == scope.session.id =>
                g(msgs, acks, obj, receivedNewSession = true)
            }
          }
        case Nil =>
          if (obj.isDefined && (!withNewSession || (withNewSession && receivedNewSession))) Tuple2(obj.get, acks)
          else f(acks, obj, receivedNewSession)
      }

      receiveOne({ data =>
        val msgs = deserializeMsgBoxes(deserializePackage(data)).map(_.body)
        g(msgs, acks, obj, receivedNewSession)
      }, { () =>
        if (withNewSession && !receivedNewSession) throw new IllegalArgumentException(s"expect NewSession")
        else throw new IllegalArgumentException(s"no messages")
      })(duration)
    }
    val (obj, ackIds) = f(Set(), None, false)
    sendMessageAck(ackIds)
    obj
  }

  def expectMsgsWhileByPF(withNewSession: Boolean = false, duration: Duration = defaultTimeout)(pf: PartialFunction[TransportMessage, Boolean])(implicit scope: TestScopeNew): Unit = {
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
        if (withNewSession && !receivedNewSession) throw new IllegalArgumentException(s"expect NewSession")
        else throw new IllegalArgumentException(s"no messages")
      })(duration)
    }
    sendMessageAck(f(Set(), false, false))
  }

  def expectMsgs(msgs: immutable.Set[TransportMessage], withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew): Unit = {
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

  def expectRpcMsg(msg: RpcResponse, withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew): Unit = {
    expectRpcMsgs(Set(msg), withNewSession, duration)
  }

  def expectRpcMsgs(msgs: immutable.Set[RpcResponse], withNewSession: Boolean = false, duration: Duration = defaultTimeout)(implicit scope: TestScopeNew): Unit = {
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
        if (withNewSession && !receivedNewSession) throw new IllegalArgumentException(s"expect NewSession")
        else throw new IllegalArgumentException(s"unreceived messages: $messages")
      })(duration)
    }
    sendMessageAck(f(msgs, Set()))
  }

  //  private def expectUpdatePush() = {
  //    ???
  //  }

  private def deserializePackage(data: ByteString)(implicit scope: TestScopeNew) = {
    val p = MTPackageBoxCodec.decodeValidValue(data).p
    if (p.authId != scope.authId || p.sessionId != scope.session.id)
      throw new IllegalArgumentException(s"p.authId(${p.authId}}) != authId(${scope.authId}) || p.sessionId(${p.sessionId}) != s.id(${scope.session.id})")
    p
  }

  private def deserializeMsgBoxes(p: TransportPackage): Seq[MessageBox] = {
    p.decodeMessageBox.toOption.get match {
      case MessageBox(_, Container(mboxes)) => mboxes
      case mb @ MessageBox(_, _) => Seq(mb)
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
