package com.secretapp.backend.services.rpc.files

import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.Request
import com.secretapp.backend.data.message.rpc.file.RequestUploadStart
import com.secretapp.backend.data.message.rpc.file.ResponseUploadStart
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.CassandraSpecification
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.ActorLikeSpecification
import org.specs2.mutable.ActorServiceHelpers
import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import scodec.codecs.{ int32 => int32codec }

class FilesServiceSpec extends RpcSpec {
  import system.dispatcher

  def requestUploadStart()(implicit probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier) = {
    val rpcReq = RpcRequestBox(Request(RequestUploadStart()))
    val messageId = rand.nextLong
    val packageBlob = pack(MessageBox(messageId, rpcReq))
    send(packageBlob)
  }

  "files service" should {
    "respond to RequestUploadStart" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        requestUploadStart()
        val rpcResp = receiveOneWithAck()
        assertResponseOk[ResponseUploadStart](rpcResp)
      }
    }
  }
}
