package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scodec.bits._
import com.secretapp.backend.models

import play.api.libs.iteratee._
import scalikejdbc._
import scala.collection.mutable
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }

abstract class FileError(val tag: String, val canTryAgain: Boolean) extends Exception
class LocationInvalid extends FileError("LOCATION_INVALID", false)
class OffsetInvalid extends FileError("OFFSET_INVALID", false)
class OffsetTooLarge extends FileError("OFFSET_TOO_LARGE", false)
class LimitInvalid extends FileError("LIMIT_INVALID", false)
class FileLost extends FileError("FILE_LOST", false)

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

  def moveToSQL(basePathStr: String)(
    implicit session: Session, dbSession: DBSession
  ): List[Throwable] = {
    val files = mutable.Map.empty[Int, java.io.RandomAccessFile]

    var prevFile: Option[java.io.RandomAccessFile] = None

    val moveIteratee =
      Iteratee.fold[Try[(String, FileBlock.EntityType)], List[Try[Unit]]](List.empty) {
        case (moves, Success((accessSalt, Entity(fileId, models.FileBlock(blockId, bytes))))) =>
          print(".")
          val move = Try {

            val raf = files.get(fileId) match {
              case None =>
                sql"""
                insert into file_datas (id, access_salt, length) VALUES (
                  ${fileId}, ${accessSalt}, ${bytes.length}
                )
                """.execute.apply

                val path = FileStorageAdapter.mkFile(fileId.toString, FileStorageAdapter.mkBasePath(basePathStr), 5)
                path.getParentFile.mkdirs()

                val x = new java.io.RandomAccessFile(path.toString, "rw")
                files.put(fileId, x)

                println("Opening")

                x
              case Some(x) =>
                sql"""
                update file_datas set length = length + ${bytes.length} where id = ${fileId}
                """.execute.apply

                println("From cache")

                x
            }

            if (prevFile.isDefined && Some(raf) != prevFile) {
              println("Closing previous")
              prevFile.get.close()
              prevFile = Some(raf)
            }

            raf.seek(blockId * FileBlock.blockSize)
            raf.write(bytes)
          }

          if (move.isFailure) {
            println(fileId, blockId)
            println(move)
          }

          moves :+ move
        case (moves, Failure(e)) =>
          moves
      }

    val tries = Await.result(blockRecord.tryEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case Failure(e) =>
        Some(e)
      case Success(_) =>
        None
    } flatten
  }
}

object File {
  def main(args: Array[String]) {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val session = DBConnector.session
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    DBConnector.flyway.migrate()
    println("migrated")

    val fileRecord = new File()

    val fails = fileRecord.moveToSQL(args.head)

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }
}

object FileStorageAdapter {
  import java.nio.file.{ Path, Paths }
  import java.net.URI

  private lazy val md = java.security.MessageDigest.getInstance("md5")

  def mkBasePath(basePathStr: String): Path = {
    val rootURI = new URI("file:///")
    Paths.get(rootURI).resolve(basePathStr)
  }

  def mkFile(name: String, basePath: Path, pathDepth: Int): java.io.File = {
    @inline
    def bytesToHex(bytes: Array[Byte], sep: String = ""): String =
      bytes.map("%02x".format(_)).mkString(sep)

    @inline
    def mkPathStr(digest: Array[Byte], depth: Int): String = {
      val (dirNameBytes, fileNameBytes) = digest.splitAt(depth)

      bytesToHex(dirNameBytes, "/") + "/" + bytesToHex(fileNameBytes)
    }

    val digest = md.digest(name.getBytes)

    val pathStr = mkPathStr(digest, pathDepth)
    basePath.resolve(pathStr).toFile
  }
}
