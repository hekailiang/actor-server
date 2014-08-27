package com.secretapp.backend.services.rpc.files

import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.Request
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.CassandraSpecification
import java.util.zip.CRC32
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.ActorLikeSpecification
import org.specs2.mutable.ActorServiceHelpers
import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._

class FilesServiceSpec extends RpcSpec {
  import system.dispatcher

  val fileContent: Array[Byte] = ((1 to (1024 * 20)) map (i => (i % 255).toByte)).toArray
  val fileSize: Int = fileContent.length
  val filecrc32: Long = {
    val crc32 = new CRC32
    crc32.update(fileContent)
    crc32.getValue
  }

  val blockSize = 0x2000

  def requestUploadStart()(
    implicit probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier): ResponseUploadStart = {
    RequestUploadStart() :~> <~:[ResponseUploadStart]
  }

  def uploadFileBlocks(config: UploadConfig)(
    implicit probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier) = {
    RequestUploadFile(config, 0, BitVector(fileContent).take(blockSize)) :~> <~:[ResponseFileUploadStarted]
    RequestUploadFile(config,
      blockSize, BitVector(fileContent).drop(blockSize).take(blockSize + blockSize)) :~> <~:[ResponseFileUploadStarted]
    RequestUploadFile(config,
      blockSize + blockSize + blockSize,
      BitVector(fileContent).drop(blockSize + blockSize + blockSize)) :~> <~:[ResponseFileUploadStarted]
  }

  "files service" should {
    "respond to RequestUploadStart" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        val config = requestUploadStart().config
        config.serverData.length should be > (0l)
      }
    }

    "respond to RequestUploadFile" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
      }
    }

    "respond with error to wrong offset in RequestUploadFile" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        val config = requestUploadStart().config
        RequestUploadFile(config, 1, BitVector(fileContent).take(blockSize)) :~> <~:(400, "OFFSET_INVALID")
      }
    }

    "respond to RequestCompleteUpload" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
        Thread.sleep(1000)
        val fileUploaded = RequestCompleteUpload(config, 3, filecrc32) :~> <~:[FileUploaded]
        Math.abs(fileUploaded.location.accessHash) should be >(0l)
      }
    }

    "respond with error to wrong blocksCount in RequestCompleteUpload" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      authDefaultUser()

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
        RequestCompleteUpload(config, 4, filecrc32) :~> <~:(400, "WRONG_BLOCKS_COUNT")
        Thread.sleep(1000)
        RequestCompleteUpload(config, 1, filecrc32) :~> <~:(400, "WRONG_BLOCKS_COUNT")
      }
    }
  }
}
