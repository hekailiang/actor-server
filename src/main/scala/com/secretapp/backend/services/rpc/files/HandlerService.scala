package com.secretapp.backend.services.rpc.files

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.Configuration
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Error, Ok }
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.{ FileBlockRecordError, FileRecord }
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon._
import java.nio.ByteBuffer
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32
import play.api.libs.iteratee._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.{ int32 => int32codec }

trait HandlerService extends GeneratorService {
  this: Handler =>

  import context.system
  import context.dispatcher
  import Configuration._

  implicit val timeout = Timeout(5.seconds)

  protected def handleRequestUploadStart(p: Package, messageId: Long)() = {
    ask(filesCounterProxy, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType] flatMap { id =>
      fileRecord.createFile(id, genFileAccessSalt) map (_ => id)
    } onComplete {
      case Success(fileId) =>
        val rsp = ResponseUploadStart(UploadConfig(int32codec.encodeValid(fileId)))
        handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
      case Failure(e) =>
        val msg = s"Failed to get next value if file id sequence: ${e.getMessage}"
        handleActor ! PackageToSend(
          p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", msg, true))).right)
        log.error(msg)
    }
  }

  protected def handleRequestUploadFile(p: Package, messageId: Long)(config: UploadConfig, offset: Int, data: BitVector) = {
    // TODO: handle failures
    val fileId = int32codec.decodeValidValue(config.serverData)

    try {
      fileRecord.write(fileId, offset, data.toByteArray) onComplete {
        case Success(_) =>

        case Failure(e) =>
          log.error("Failed to upload file chunk {} {} {}", p, messageId, e)
      }
      val rsp = ResponseFileUploadStarted()
      handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
    } catch {
      case e: FileBlockRecordError =>
        handleActor ! PackageToSend(
          p.replyWith(messageId, RpcResponseBox(messageId, Error(400, e.tag, "", e.canTryAgain))).right)
    }
  }

  protected lazy val inputCRC32: Iteratee[ByteBuffer, CRC32] = {
    Iteratee.fold[ByteBuffer, CRC32](new CRC32) {
      (crc32, buffer) =>
        crc32.update(buffer)
        crc32
    }
  }

  protected def handleRequestCompleteUpload(p: Package, messageId: Long)(config: UploadConfig, blocksCount: Int, crc32: Long) = {
    val fileId = int32codec.decodeValidValue(config.serverData)
    val faccessHash = getAccessHash(fileId)

    fileRecord.countSourceBlocks(fileId) onComplete {
      case Success(sourceBlocksCount) =>
        if (blocksCount == sourceBlocksCount) {
          Iteratee.flatten(fileRecord.blocksByFileId(fileId) |>> inputCRC32).run map (_.getValue) onComplete {
            case Success(realcrc32) =>
              if (crc32 == realcrc32) {
                faccessHash onComplete {
                  case Success(accessHash) =>
                    val rsp = FileUploaded(FileLocation(fileId, accessHash))
                    handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
                  case Failure(e) =>
                    log.error("Failed to get file accessHash")
                    handleActor ! PackageToSend(
                      p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", "", true))).right)
                    throw e
                }
              } else {
                handleActor ! PackageToSend(
                  p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "WRONG_CRC", "", true))).right)
              }
            case Failure(e) =>
              log.error("Failed to calculate file crc32")
              handleActor ! PackageToSend(
                p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", "", true))).right)
              throw e
          }
        } else {
          handleActor ! PackageToSend(
            p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "WRONG_BLOCKS_COUNT", "", true))).right)
        }
      case Failure(e) =>
        log.error("Failed to get source blocks count")
        handleActor ! PackageToSend(
          p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", "", true))).right)
        throw e
    }
  }

  protected def getAccessHash(fileId: Int): Future[Long] = {
    fileRecord.getFileAccessSalt(fileId) map { accessSalt =>
      val str = s"$fileId:$accessSalt:$secretKey"
      val digest = MessageDigest.getInstance("MD5")
      val res = digest.digest(str.getBytes)
      ByteBuffer.wrap(res).getLong
    }
  }
}
