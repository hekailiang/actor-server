package com.secretapp.backend.persist

import im.actor.server.persist.file.adapter.FileAdapter
import play.api.libs.iteratee.Enumerator
import scala.concurrent._

abstract class FileError(val tag: String, val canTryAgain: Boolean) extends Exception
class LocationInvalid extends FileError("LOCATION_INVALID", false)
class OffsetInvalid extends FileError("OFFSET_INVALID", false)
class OffsetTooLarge extends FileError("OFFSET_TOO_LARGE", false)
class LimitInvalid extends FileError("LIMIT_INVALID", false)
class FileLost extends FileError("FILE_LOST", false)

object File {
  val blockSize = 8 * 1024 // 8 kB

  private def limitValid(limit: Int) = limit > 0 && limit % 1024 == 0 && limit <= 512 * 1024
  private def offsetValid(offset: Int) = offset >= 0 && offset % blockSize == 0

  private def withValidOffset[A](offset: Int)(f: A): A = {
    if (!offsetValid(offset))
      throw new OffsetInvalid

    f
  }

  private def withValidOL[A](offset:Int, limit: Int)(f: => A): A = withValidOffsetLimit(offset, limit)(f)

  private def withValidOffsetLimit[A](offset: Int, limit: Int)(f: => A): A = {
    if (!offsetValid(offset))
      throw new OffsetInvalid

    if (!limitValid(limit))
      throw new LimitInvalid

    f
  }

  def create(fa: FileAdapter, id: Long, accessSalt: String)(
    implicit ec: ExecutionContext
  ): Future[Unit] = {
    FileData.create(id, accessSalt) map (_ => ())
  }

  def write(fa: FileAdapter, id: Long, offset: Int, data: Array[Byte])(
    implicit ec: ExecutionContext
  ): Future[Unit] = withValidOffset(offset) {
    for {
      _ <- fa.write(id.toString, offset, data)
      _ <- Future.sequence(Seq(
        FileData.incrementLength(id, data.length),
        FileBlock.createIfNotExists(id, offset, data.length)
      ))
    } yield ()
  }

  def read(fa: FileAdapter, id: Long, offset: Int, limit: Int)(
    implicit ec: ExecutionContext
  ): Future[Array[Byte]] = withValidOL(offset, limit) {
    fa.read(id.toString, offset, limit)
  }

  def read(fa: FileAdapter, id: Long)(
    implicit ec: ExecutionContext
  ): Enumerator[Array[Byte]] = {
    fa.read(id.toString)
  }

  def readAll(fa: FileAdapter, id: Long)(
    implicit ec: ExecutionContext
  ): Future[Array[Byte]] = {
    fa.readAll(id.toString)
  }

  def complete(fa: FileAdapter, id: Long)(
    implicit ec: ExecutionContext
  ): Future[Boolean] = {
    FileBlock.deleteAll(id)
  }
}

/*package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scala.concurrent.{ExecutionContext, Future}
import scodec.bits._
import com.secretapp.backend.models



class File(implicit session: Session, context: ExecutionContext with Executor) {
  private lazy val blockRecord = new FileBlock
  private lazy val sourceBlockRecord = new FileSourceBlock

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
          sourceBlockRecord.insertEntity(models.FileSourceBlock(id, offset, bytes.length))
      }
    }
    f
  }

  def getFileAccessSalt(fileId: Int): Future[String] =
    blockRecord.select(_.accessSalt).where(_.fileId eqs fileId).one() map {
      _.getOrElse(throw new LocationInvalid)
    }

  def getFile(fileId: Int, offset: Int, limit: Int): Future[Array[Byte]] = {
    for {
      blocks <- blockRecord.getFileBlocks(fileId, offset, limit)
    } yield {
      // FIXME: don't use BitVector here
      val bytes = blocks.foldLeft(Vector.empty[Byte])((a, b) => a ++ BitVector(b).toByteArray)
      bytes.drop(offset % FileBlock.blockSize).take(limit).toArray
    }
  }

  def getFile(fileId: Int): Future[Array[Byte]] = {
    blockRecord.getBlocksLength(fileId) flatMap (getFile(fileId, 0, _))
  }

  val getFileLength = sourceBlockRecord.getFileLength _

  def blocksByFileId(fileId: Int) = blockRecord.blocksByFileId(fileId)

  def countSourceBlocks(fileId: Int) = sourceBlockRecord.countBlocks(fileId)
}
 */
