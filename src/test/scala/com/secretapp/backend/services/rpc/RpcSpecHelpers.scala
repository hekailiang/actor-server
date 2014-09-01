package com.secretapp.backend.services.rpc

import akka.actor._
import akka.io.Tcp.Received
import akka.testkit.TestProbe
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
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

  case class RpcTestScope(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, user: User)
  object RpcTestScope {
    def pair(): (RpcTestScope, RpcTestScope) = {
      (apply(1, 79632740769L), apply(2, 79853867016L))
    }

    def apply(): RpcTestScope = apply(1, 79632740769L)

    def apply(uid: Int, phone: Long): RpcTestScope = {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      val user = authDefaultUser()
      RpcTestScope(probe, apiActor, session, user)
    }
  }

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
    def :~>!(implicit scope: RpcTestScope): Unit = {
      implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      val r = pack(boxed(m))
      probe.send(destActor, Received(r.blob))
    }

    /**
      * Sends message, waits for reply, checks its type and returns the reply
      */
    def :~>?[A <: RpcResponseMessage : ClassTag](klass: Class[A])(implicit scope: RpcTestScope): A = {
      implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }

    /**
      * Sends message, asserts reply is error and checks its params
      */
    def :~>(wp: WrappedReceiveResponseError)(implicit scope: RpcTestScope) = {
      implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      val error = receiveOneWithAck().assertResponseError
      wp.f(error)
    }

    /**
      * Sends message, asserts reply is Ok and returns it
      */
    def :~>[A <: RpcResponseMessage : ClassTag](wp: WrappedReceiveResponseOk[A])(implicit scope: RpcTestScope): A = {
      implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
      :~>!
      receiveOneWithAck().assertResponseOk[A]
    }
  }

  def <~:[A <: RpcResponseMessage : ClassTag]: WrappedReceiveResponseOk[A] = {
    //implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
    new WrappedReceiveResponseOk[A]()
  }

  def <~:(errorCode: Int, errorTag: String): WrappedReceiveResponseError = {
    //implicit val RpcTestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: User) = scope
    WrappedReceiveResponseError { error =>
      error.code must equalTo(errorCode)
      error.tag must equalTo(errorTag)
    }
  }
}
