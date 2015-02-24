package com.secretapp.backend.services.rpc.files

import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.persist
import com.secretapp.backend.services.GeneratorService
import java.nio.ByteBuffer
import java.util.zip.CRC32
import com.secretapp.backend.util.ACL
import play.api.libs.iteratee._
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import scodec.bits._
import scodec.codecs.{ int32 => int32codec, int64 => int64codec }

trait HandlerService extends GeneratorService {
  this: Handler =>

  import context.dispatcher
  import context.system

  implicit val timeout = Timeout(5.seconds)

  protected def handleRequestStartUpload(): Future[RpcResponse] = {
    val fileId = rand.nextLong
    persist.File.create(fileAdapter, fileId, genFileAccessSalt) map { _ =>
      log.debug("Created file {}", fileId)
      val rsp = ResponseStartUpload(UploadConfig(int64codec.encodeValid(fileId)))
      Ok(rsp)
    }
  }

  protected def decodeServerData(data: BitVector): String \/ Long = {
    // for compatibility with old serverData which contained int32-decoded file ids
    (int64codec.decodeValue(data) ||| int32codec.decodeValue(data)) map (_.asInstanceOf[Long])
  }

  protected def handleRequestUploadFile(config: UploadConfig, offset: Int, data: BitVector): Future[RpcResponse] = {
    decodeServerData(config.serverData) match {
      case -\/(e) =>
        log.warning(s"Cannot parse serverData $e")
        Future.successful(Error(400, "CONFIG_INCORRECT", "", false))
      case \/-(fileId) =>
        try {
          log.debug("Writing file id: {} with offset: {}", fileId, offset)
          for {
            _ <- persist.File.write(fileAdapter, fileId, offset, data.toByteArray)
          } yield {
            val rsp = ResponseVoid()
            Ok(rsp)
          }
        } catch {
          case e: persist.FileError =>
            Future.successful(Error(400, e.tag, "", e.canTryAgain))
        }
    }
  }

  protected def inputCRC32: Iteratee[Array[Byte], CRC32] = {
    Iteratee.fold[Array[Byte], CRC32](new CRC32) {
      (crc32, data) =>
        crc32.update(data)
        crc32
    }
  }

  // TODO: refactor makaron
  protected def handleRequestCompleteUpload(config: UploadConfig, blocksCount: Int, crc32: Long): Future[RpcResponse] = {
    decodeServerData(config.serverData) match {
      case -\/(e) =>
        log.warning(s"Cannot parse serverData $e")
        Future.successful(Error(400, "CONFIG_INCORRECT", "", false))
      case \/-(fileId) =>
        val blocksCountFuture = persist.FileBlock.countAll(fileId)
        val dataOptFuture = persist.FileData.find(fileId)

        val countAndDataOptFuture = for {
          count <- blocksCountFuture
          dataOpt <- dataOptFuture
        } yield {
          dataOpt map ((count, _))
        }

        countAndDataOptFuture flatMap {
          case Some((uploadedBlocksCount, models.FileData(_, accessSalt, _, _))) =>
            val accessHash = ACL.fileAccessHash(fileId, accessSalt)

            if (blocksCount == uploadedBlocksCount) {
              persist.File.read(fileAdapter, fileId) flatMap { enumerator =>
                val f: Future[RpcResponse] = Iteratee.flatten(enumerator |>> inputCRC32).run map (_.getValue) map { realcrc32 =>
                  persist.File.complete(fileAdapter, fileId)

                  if (crc32 == realcrc32) {
                    val rsp = ResponseCompleteUpload(models.FileLocation(fileId, accessHash))
                    Ok(rsp)
                  } else {
                    Error(400, "WRONG_CRC", "", true)
                  }
                }
                f onFailure {
                  case e: Throwable =>
                    log.error("Failed to calculate file crc32")
                    throw e
                }
                f
              }
            } else {
              Future.successful(Error(400, "WRONG_BLOCKS_COUNT", "", true))
            }
          case None =>
            throw new persist.LocationInvalid
        }
    }
  }

  protected def handleRequestGetFile(location: models.FileLocation, offset: Int, limit: Int): Future[RpcResponse] = {
    persist.FileData.find(location.fileId) flatMap {
      case Some(models.FileData(fileId, accessSalt, _, _)) =>
        val realAccessHash = ACL.fileAccessHash(fileId, accessSalt)
        if (realAccessHash != location.accessHash) {
          throw new persist.LocationInvalid
        }

        try {
          log.debug("Reading file id: {} offset: {} limit: {}", fileId, offset, limit)

          val f = persist.File.read(fileAdapter, fileId, offset, limit).map { bytes =>
            val rsp = ResponseGetFile(BitVector(bytes))
            Ok(rsp)
          }

          f onFailure {
            case e: Throwable =>
              log.error(e, "Failed to read file id: {} offset: {} limit: {}", fileId, offset, limit)
          }

          f
        } catch {
          case e: persist.FileError =>
            Future successful Error(400, e.tag, "", e.canTryAgain)
        }

      case None =>
        Future.successful(Error(400, "LOCATION_INVALID", "", true))
    }
  }
}
