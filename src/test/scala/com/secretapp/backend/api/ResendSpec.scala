package com.secretapp.backend.api

import akka.io.Tcp.{ Received, Write }
import akka.util.ByteString
import com.secretapp.backend.data.message.MessageAck
import com.secretapp.backend.data.message.{ Ping, Pong }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.data.transport.MTPackageBox
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.transport.MTPackageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import scala.concurrent.duration._

class ResendServiceSpec extends RpcSpec {
  implicit val duration = DurationInt(5).seconds

  "session" should {
    "resend unackd messages" in {
      val scopeOrigin = TestScope()

      {
        implicit val scope = scopeOrigin

        RequestGetState() :~>!

        val write = scope.probe.receiveOne(duration).asInstanceOf[Write]
        val p = MTPackageBoxCodec.decodeValidValue(write.data).p
        val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)

        mb.body should beAnInstanceOf[MessageAck]

        scope.apiActor.tell(write.ack, scope.probe.ref)
      }

      Thread.sleep(5000) // let request to be handled

      {
        implicit val scope = scopeOrigin.reconnect()

        val received = Received(
          ByteString(
            MTPackageBoxCodec.encodeValid(
              MTPackageBox(0,
                MTPackage(scope.user.authId, scope.session.id,
                  MessageBoxCodec.encodeValid(MessageBox(10L, Ping(1L)))))).toByteArray))

        scope.probe.send(scope.apiActor, received)

        val m1 = {
          // receive ack
          val write = scope.probe.receiveOne(duration).asInstanceOf[Write]
          scope.apiActor.tell(write.ack, scope.probe.ref)

          val p = MTPackageBoxCodec.decodeValidValue(write.data).p
          val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)

          mb.body
        }

        val m2 = {
          val write = scope.probe.receiveOne(duration).asInstanceOf[Write]
          scope.apiActor.tell(write.ack, scope.probe.ref)

          val p = MTPackageBoxCodec.decodeValidValue(write.data).p
          val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)

          mb.body
        }

        val m3 = {
          val write = scope.probe.receiveOne(duration).asInstanceOf[Write]
          scope.apiActor.tell(write.ack, scope.probe.ref)

          val p = MTPackageBoxCodec.decodeValidValue(write.data).p
          val mb = MessageBoxCodec.decodeValidValue(p.messageBoxBytes)

          mb.body
        }

        val messages = List(m1, m2, m3)

        (messages.filter {
          case m: MessageAck => true
          case _ => false
        } length) should beEqualTo(1)

        (messages.filter {
          case m: RpcResponseBox => true
          case _ => false
        } length) should beEqualTo(1)

        (messages.filter {
          case m: Pong => true
          case _ => false
        } length) should beEqualTo(1)

        scope.probe.expectNoMsg(duration)
      }
    }
  }
}
