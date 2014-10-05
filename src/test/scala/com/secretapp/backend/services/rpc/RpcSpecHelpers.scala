package com.secretapp.backend.services.rpc

import akka.actor._
import akka.io.Tcp.Received
import akka.testkit.TestProbe
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import org.scalamock.specs2.MockFactory
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable._
import scala.reflect.ClassTag

trait RpcSpecHelpers {
  self: RpcSpec =>

  implicit class AnyRefWithSpecHelpers(x: AnyRef) {
    def assertInstanceOf[A: ClassTag]: A = {
      x should beAnInstanceOf[A]
      x.asInstanceOf[A]
    }
  }

  implicit class MessageBoxWithSpecHelpers(m: MessageBox) {
    def assertResponseOk[A <: RpcResponseMessage : ClassTag ]: A = {
      m
        .body.assertInstanceOf[RpcResponseBox]
        .body.assertInstanceOf[Ok]
        .body.assertInstanceOf[A]
    }

    def assertResponseError: Error = {
      m
        .body.assertInstanceOf[RpcResponseBox]
        .body.assertInstanceOf[Error]
    }
  }

  trait WrappedReceiveResponse
  case class WrappedReceiveResponseError(f: (Error) => Unit) extends WrappedReceiveResponse
  class WrappedReceiveResponseOk[A] extends WrappedReceiveResponse

  implicit class RpcRequestMessageWithSpecHelpers(m: RpcRequestMessage) {
    def boxed(msg: RpcRequestMessage)(implicit s: SessionIdentifier) = {
      RpcRequestBox(Request(msg))
    }

    /**
      * Sends message
      */
    def :~>!(implicit scope: TestScope): Unit = {
      implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      // TODO: real index
      val r = pack(0, u.authId, boxed(m))
      probe.send(destActor, Received(r.blob))
    }

    /**
      * Sends message, waits for reply, checks its type and returns the reply
      */
    def :~>?[A <: RpcResponseMessage : ClassTag](klass: Class[A])(implicit scope: TestScope): A = {
      implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }

    /**
      * Sends message, asserts reply is error and checks its params
      */
    def :~>(wp: WrappedReceiveResponseError)(implicit scope: TestScope) = {
      implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      val error = receiveOneWithAck().assertResponseError
      wp.f(error)
    }

    /**
      * Sends message, asserts reply is Ok and returns it
      */
    def :~>[A <: RpcResponseMessage : ClassTag]
      (wp: WrappedReceiveResponseOk[A], updates: Seq[UpdateBox] = Seq.empty)
      (implicit scope: TestScope): (A, Seq[UpdateBox]) = {
      implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      val msgs = receiveNWithAck(1)

      val (resps, newUpdates) = msgs partition {
        case MessageBox(_, ub: UpdateBox) => false
        case _ => true
      }

      val newUpdateBoxes = newUpdates map (_.body.asInstanceOf[UpdateBox])

      if (resps.length == 0) {
        :~>(wp, updates ++ newUpdateBoxes)
      } else if (resps.length == 1) {
        (resps.head.assertResponseOk[A], updates ++ newUpdateBoxes)
      } else {
        throw new Exception(s"Received more than one response $resps")
      }
    }
  }

  def <~:[A <: RpcResponseMessage : ClassTag]: WrappedReceiveResponseOk[A] = {
    //implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
    new WrappedReceiveResponseOk[A]()
  }

  def <~:(errorCode: Int, errorTag: String): WrappedReceiveResponseError = {
    //implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
    WrappedReceiveResponseError { error =>
      error.code must equalTo(errorCode)
      error.tag must equalTo(errorTag)
    }
  }
}
