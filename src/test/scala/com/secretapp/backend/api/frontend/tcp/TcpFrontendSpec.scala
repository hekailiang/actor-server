package com.secretapp.backend.api.frontend.tcp

import com.secretapp.backend.api.frontend.MTConnection
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport._
import akka.util.ByteString
import akka.io.Tcp._
import scodec.bits._
import scala.collection.mutable

class TcpFrontendSpec extends RpcSpec {
  import system.dispatcher

  "actor" should {
    "send drop to package with invalid crc" in {
      implicit val (probe, apiActor) = probeAndActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411"
      probe.send(apiActor, Received(req))
      val res = protoPackageBox.build(0, 0L, 0L, 0L, Drop(0L, "invalid crc32"))
      probe.expectMsgPF() {
        case Write(data, _) => data == res
      }
    }

    "parse packages in single stream" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      implicit val authId = rand.nextLong()
      implicit val transport = MTConnection

      insertAuthId(authId)
      var msgId = 0L
      val pingValQueue = mutable.Set[Long]()
      val messages = (1 to 100).map { _ =>
        val pingVal = rand.nextLong()
        pingValQueue += pingVal
        msgId += 4
        MessageBox(msgId, Ping(pingVal))
      }
      val packages = messages.map(pack(0, authId, _))
      val req = packages.map(_.blob).foldLeft(ByteString.empty)(_ ++ _)
      req.grouped(7) foreach { buf =>
        probe.send(apiActor, Received(buf))
      }
      expectMsgsWhileByPF(withNewSession = true) {
        case Pong(pingVal) =>
          pingValQueue -= pingVal
          !pingValQueue.isEmpty
      }
    }
  }
}
