package com.secretapp.backend.services.rpc

import akka.actor.ActorRef
import akka.io.Tcp.Received
import akka.testkit.TestProbe
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.MessageBox
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.ActorServiceHelpers
import scala.reflect.ClassTag

trait RpcSpecHelpers extends ShouldMatchers {
  self: ActorServiceHelpers =>

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
    def :~>!(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): Unit = {
      val r = pack(boxed(m))
      probe.send(destActor, Received(r.blob))
    }

    /**
      * Sends message, waits for reply, checks its type and returns the reply
      */
    def :~>?[A <: RpcResponseMessage : ClassTag](klass: Class[A])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): A = {
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }

    /**
      * Sends message, asserts reply is error and checks its params
      */
    def :~>(wp: WrappedReceiveResponseError)(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier) = {
      :~>!
      val error = receiveOneWithAck().assertResponseError
      wp.f(error)
    }

    /**
      * Sends message, asserts reply is error and checks its params
      */
    def :~>[A <: RpcResponseMessage : ClassTag](wp: WrappedReceiveResponseOk[A])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): A = {
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }
  }

  def @<~:[A <: RpcResponseMessage : ClassTag](implicit probe: TestProbe, s: SessionIdentifier): WrappedReceiveResponseOk[A] =
    new WrappedReceiveResponseOk[A]()

  def @<~:(errorCode: Int, errorTag: String)(implicit probe: TestProbe, s: SessionIdentifier): WrappedReceiveResponseError =
    WrappedReceiveResponseError { error =>
      error.code should equalTo(errorCode)
      error.tag should equalTo(errorTag)
    }
}
