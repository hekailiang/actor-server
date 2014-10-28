package com.secretapp.backend.persist

import java.security.MessageDigest
import com.secretapp.backend.Configuration
import com.websudos.phantom.Implicits._
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import scala.concurrent.{ExecutionContext, Future}
import scodec.bits._

case class File(fileId: Int, sourceBlocksCount: Int)

abstract class FileRecordError(val tag: String, val canTryAgain: Boolean) extends Exception
class LocationInvalid extends FileRecordError("LOCATION_INVALID", false)
class OffsetInvalid extends FileRecordError("OFFSET_INVALID", false)
class OffsetTooLarge extends FileRecordError("OFFSET_TOO_LARGE", false)
class LimitInvalid extends FileRecordError("LIMIT_INVALID", false)
class FileLost extends FileRecordError("FILE_LOST", false)

class FileRecord(implicit session: Session, context: ExecutionContext with Executor) {

  import Configuration._

  private lazy val blockRecord = new FileBlockRecord
  private lazy val sourceBlockRecord = new FileSourceBlockRecord

  def createTable(session: Session): Future[Unit] = {
    val b = blockRecord.createTable(session)
    val sb = sourceBlockRecord.createTable(session)
    for {
      _ <- b
      _ <- sb
    } yield Unit
  }

  def truncateTable(session: Session): Future[Unit] = {
    val b = blockRecord.truncateTable(session)
    val sb = sourceBlockRecord.truncateTable(session)
    for {
      _ <- b
      _ <- sb
    } yield Unit
  }

  def createFile(id: Int, accessSalt: String): Future[ResultSet] = {
    blockRecord.insert.value(_.fileId, id).value(_.accessSalt, accessSalt).future()
  }

  def write(id: Int, offset: Int, bytes: Array[Byte], isSourceBlock: Boolean = true) = {
    val f = blockRecord.write(id, offset, bytes)
    if (isSourceBlock) {
      f onSuccess {
        case _ =>
          sourceBlockRecord.insertEntity(FileSourceBlock(id, offset, bytes.length))
      }
    }
    f
  }

  def getFileAccessSalt(fileId: Int): Future[String] =
    blockRecord.select(_.accessSalt).where(_.fileId eqs fileId).one() map {
      _.getOrElse(throw new LocationInvalid)
    }

  def getFile(fileId: Int, offset: Int, limit: Int): Future[Array[Byte]] = {
    println(s"getFile $offset $limit")
    for {
      blocks <- blockRecord.getFileBlocks(fileId, offset, limit)
    } yield {
      // FIXME: don't use BitVector here
      val bytes = blocks.foldLeft(Vector.empty[Byte])((a, b) => a ++ BitVector(b).toByteArray)
      bytes.drop(offset % FileBlockRecord.blockSize).take(limit).toArray
    }
  }

  def getFile(fileId: Int): Future[Array[Byte]] = {
    blockRecord.getBlocksLength(fileId) flatMap (getFile(fileId, 0, _))
  }

  val getFileLength = sourceBlockRecord.getFileLength _

  def blocksByFileId(fileId: Int) = blockRecord.blocksByFileId(fileId)

  def countSourceBlocks(fileId: Int) = sourceBlockRecord.countBlocks(fileId)

  def getAccessHash(fileId: Int): Future[Long] = {
    getFileAccessSalt(fileId) map { accessSalt =>
      val str = s"$fileId:$accessSalt:$secretKey"
      val res = MessageDigest.getInstance("MD5").digest(str.getBytes)
      ByteBuffer.wrap(res).getLong
    }
  }
}
