package com.secretapp.backend.services.rpc.files

import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Ok, ResponseVoid }
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.Request
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.transport.MessageBox
import java.util.zip.CRC32
import org.specs2.mutable.ActorLikeSpecification
import org.specs2.mutable.ActorServiceHelpers
import akka.actor._
import akka.testkit._
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._

class FilesServiceSpec extends RpcSpec {
  import system.dispatcher

  val fileContent: Array[Byte] = ((1 to (1024 * 40)) map (i => (i % 255).toByte)).toArray
  val fileSize: Int = fileContent.length
  val filecrc32: Long = {
    val crc32 = new CRC32
    crc32.update(fileContent)
    crc32.getValue
  }

  val blockSize = 0x2000

  def requestUploadStart()(
    implicit scope: TestScope): ResponseStartUpload = {
    val (rsp, _) = RequestStartUpload() :~> <~:[ResponseStartUpload]
    rsp
  }

  def uploadFileBlocks(config: UploadConfig)(
    implicit scope: TestScope) = {
    RequestUploadPart(config,
      blockSize, BitVector(fileContent.drop(blockSize).take(blockSize + blockSize))) :~> <~:[ResponseVoid]
    RequestUploadPart(config, 0, BitVector(fileContent.take(blockSize))) :~> <~:[ResponseVoid]
    RequestUploadPart(config,
      blockSize, BitVector(fileContent.drop(blockSize).take(blockSize + blockSize))) :~> <~:[ResponseVoid]
    RequestUploadPart(config,
      blockSize + blockSize + blockSize,
      BitVector(fileContent.drop(blockSize + blockSize + blockSize))) :~> <~:[ResponseVoid]
  }

  "files service" should {
    "respond to RequestUploadStart" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      {
        val config = requestUploadStart().config
        config.serverData.length should be > (0l)
      }
    }

    "respond to RequestUploadFile" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      {
        val config = requestUploadStart().config
        RequestUploadPart(config, 1, BitVector(fileContent).take(blockSize)) :~> <~:(400, "OFFSET_INVALID")
        uploadFileBlocks(config)
      }
    }

    "respond to RequestCompleteUpload" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
        Thread.sleep(3000)
        val (fileUploaded, _) = RequestCompleteUpload(config, 3, filecrc32) :~> <~:[ResponseCompleteUpload]
        Math.abs(fileUploaded.location.accessHash) should be >(0l)

        //RequestCompleteUpload(config, 4, filecrc32) :~> <~:(400, "WRONG_BLOCKS_COUNT")
        //RequestCompleteUpload(config, 1, filecrc32) :~> <~:(400, "WRONG_BLOCKS_COUNT")
      }
    }

    "upload two files in a row" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
        Thread.sleep(3000)
        val fileUploaded = RequestCompleteUpload(config, 3, filecrc32) :~> <~:[ResponseCompleteUpload]
      }

      {
        val config = requestUploadStart().config
        uploadFileBlocks(config)
        Thread.sleep(3000)
        val fileUploaded = RequestCompleteUpload(config, 3, filecrc32) :~> <~:[ResponseCompleteUpload]
      }
    }

    "respond to RequestGetFile" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      val config = requestUploadStart().config
      uploadFileBlocks(config)
      Thread.sleep(1000)
      val (fileUploaded, _) = RequestCompleteUpload(config, 3, filecrc32) :~> <~:[ResponseCompleteUpload]

      {
        val (filePart, _) = RequestGetFile(fileUploaded.location, 0, blockSize) :~> <~:[ResponseGetFile]
        filePart.data.toByteArray.length should equalTo(blockSize)
        filePart.data.toByteArray should equalTo(fileContent.take(blockSize))
      }

      {
        val (filePart, _) = RequestGetFile(fileUploaded.location, blockSize, blockSize * 3) :~> <~:[ResponseGetFile]
        filePart.data.toByteArray.length should equalTo(blockSize * 3)
        filePart.data.toByteArray should equalTo(fileContent.drop(blockSize).take(blockSize * 3))
      }

      RequestGetFile(fileUploaded.location, blockSize + 1, blockSize * 3) :~> <~:(400, "OFFSET_INVALID")
      RequestGetFile(fileUploaded.location, blockSize, blockSize * 3 + 1) :~> <~:(400, "LIMIT_INVALID")
      RequestGetFile(fileUploaded.location, blockSize, -blockSize) :~> <~:(400, "LIMIT_INVALID")
      RequestGetFile(fileUploaded.location, blockSize, 1024 * 1024) :~> <~:(400, "LIMIT_INVALID")
    }
  }
}
