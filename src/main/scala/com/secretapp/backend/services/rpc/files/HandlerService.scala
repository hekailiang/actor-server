package com.secretapp.backend.services.rpc.files

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.Configuration
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.persist.LocationInvalid
import com.secretapp.backend.persist.{ FileRecordError, FileRecord }
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon._
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

  protected def handleRequestUploadStart(): Future[RpcResponse] = {
    ask(filesCounterProxy, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType] flatMap { id =>
      fileRecord.createFile(id, genFileAccessSalt) map (_ => id)
    } map { fileId =>
      val rsp = ResponseUploadStarted(UploadConfig(int32codec.encodeValid(fileId)))
      Ok(rsp)
    }
  }

  protected def handleRequestUploadFile(config: UploadConfig, offset: Int, data: BitVector): Future[RpcResponse] = {
    // TODO: handle failures
    val fileId = int32codec.decodeValidValue(config.serverData)

    try {
      fileRecord.write(fileId, offset, data.toByteArray) map { _ =>
        val rsp = ResponseVoid()
        Ok(rsp)
      }
    } catch {
      case e: FileRecordError =>
        Future.successful(Error(400, e.tag, "", e.canTryAgain))
    }
  }

  protected def readBuffer(buffer: ByteBuffer): Array[Byte] = {
    val bytes: Array[Byte] = new Array(buffer.remaining)
    buffer.get(bytes, 0, bytes.length)
    bytes
  }

  protected def inputCRC32: Iteratee[ByteBuffer, CRC32] = {
    Iteratee.fold[ByteBuffer, CRC32](new CRC32) {
      (crc32, buffer) =>
        crc32.update(readBuffer(buffer))
        crc32
    }
  }

  protected def handleRequestCompleteUpload(config: UploadConfig, blocksCount: Int, crc32: Long): Future[RpcResponse] = {
    val fileId = int32codec.decodeValidValue(config.serverData)
    val faccessHash = fileRecord.getAccessHash(fileId)

    val f = fileRecord.countSourceBlocks(fileId) flatMap { sourceBlocksCount =>
      if (blocksCount == sourceBlocksCount) {
        val f = Iteratee.flatten(fileRecord.blocksByFileId(fileId) |>> inputCRC32).run map (_.getValue) flatMap { realcrc32 =>
          if (crc32 == realcrc32) {
            val f = faccessHash map { accessHash =>
                val rsp = ResponseUploadCompleted(FileLocation(fileId, accessHash))
                Ok(rsp)
            }
            f onFailure {
              case e: Throwable =>
                log.error("Failed to get file accessHash")
                throw e
            }
            f
          } else {
            Future.successful(Error(400, "WRONG_CRC", "", true))
          }
        }
        f onFailure {
          case e: Throwable =>
            log.error("Failed to calculate file crc32")
            throw e
        }
        f
      } else {
        Future.successful(Error(400, "WRONG_BLOCKS_COUNT", "", true))
      }
    }
    f onFailure {
      case e: Throwable =>
        log.error("Failed to get source blocks count")
        throw e
    }
    f
  }

  protected def handleRequestGetFile(location: FileLocation, offset: Int, limit: Int): Future[RpcResponse] = {
    // TODO: Int vs Long
    val f = fileRecord.getAccessHash(location.fileId.toInt) flatMap { realAccessHash =>
      if (realAccessHash != location.accessHash) {
        throw new LocationInvalid
      }
      fileRecord.getFile(location.fileId.toInt, offset, limit)
    } map { bytes =>
        val rsp = ResponseFilePart(BitVector(bytes))
        Ok(rsp)
    } recover {
      case e: FileRecordError =>
        Error(400, e.tag, "", e.canTryAgain)
    }
    f onFailure {
      case e: Throwable =>
        log.error("Failed to check file accessHash")
        throw e
    }
    f
  }
}
