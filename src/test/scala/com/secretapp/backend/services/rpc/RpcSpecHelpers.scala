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
  }

  implicit class RpcRequestMessageWithSpecHelpers(m: RpcRequestMessage) {
    def boxed(msg: RpcRequestMessage)(implicit s: SessionIdentifier) = {
      RpcRequestBox(Request(msg))
    }

    /**
      * Sends message, waits for reply, checks its type and returns the reply
      */
    def :~>?[A <: RpcResponseMessage : ClassTag](klass: Class[A])(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): A = {
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }

    /**
      * Sends message
      */
    def :~>!(implicit probe: TestProbe, destActor: ActorRef, s: SessionIdentifier): Unit = {
      val r = pack(boxed(m))
      probe.send(destActor, Received(r.blob))
    }
  }
}
