package im.actor.server.persist.file.adapter.fs

import akka.actor._
import akka.util.ByteString
import java.io.RandomAccessFile
import java.net.URI
import java.io.File
import java.nio.file.{ Files, Paths }
import java.security.MessageDigest
import scala.collection.immutable
import scala.collection.JavaConversions._
import scala.concurrent._, duration._
import scala.util.{ Try, Success, Failure }

object FileStorageProtocol {
  sealed trait Message

  @SerialVersionUID(1L)
  case class Write(name: String, offset: Int, bytes: ByteString) extends Message

  @SerialVersionUID(1L)
  case object Wrote extends Message

  @SerialVersionUID(1L)
  case class CloseRW(name: String) extends Message

  @SerialVersionUID(1L)
  case class CloseR(name: String) extends Message

  @SerialVersionUID(1L)
  case class Read(name: String, offset: Int, length: Int) extends Message

  @SerialVersionUID(1L)
  case class ReadBytes(name: String, bytes: ByteString) extends Message

  @SerialVersionUID(1L)
  case object FileNotExists extends Message
}

object FileStorageActor {
  def props(closeTimeout: FiniteDuration, basePathStr: String, pathDepth: Int): Props = {
    val maxPathDepth = 15

    if (pathDepth > maxPathDepth)
      throw new Error(s"PathDepth can not be more than $maxPathDepth")

    Props(classOf[FileStorageActor], closeTimeout, basePathStr, pathDepth)
  }
}

class FileStorageActor(closeTimeout: FiniteDuration, basePathStr: String, pathDepth: Int = 6) extends Actor {
  import context.dispatcher
  import FileStorageProtocol._

  type FileWithCloser = (RandomAccessFile, Cancellable)

  private var filesRW: immutable.Map[String, FileWithCloser] = immutable.Map.empty
  private var filesR: immutable.Map[String, FileWithCloser] = immutable.Map.empty
  private val md = MessageDigest.getInstance("md5")

  private val rootURI = new URI("file:///")
  private val basePath = Paths.get(rootURI).resolve(basePathStr)

  def receive = {
    case Write(name, offset, bytes) =>
      val raf = getOrOpenRW(name)

      val requiredLength = offset + bytes.length

      if (raf.length < requiredLength)
        raf.setLength(requiredLength)

      raf.seek(offset)
      raf.write(bytes.toArray)

      rescheduleCloseRW(name)

      sender ! Wrote
    case Read(name, offset, length) =>
      getOrOpenR(name) match {
        case Success(raf) =>
          raf.seek(offset)
          val bytes = read(raf, offset, length)

          sender ! ReadBytes(name, ByteString(bytes))

          rescheduleCloseR(name)
        case Failure(_) =>
          sender ! FileNotExists
      }
    case CloseRW(name) =>
      filesRW.get(name) map {
        case (raf, _) =>
          raf.close
          filesRW = filesRW - name
      }
    case CloseR(name) =>
      filesR.get(name) map {
        case (raf, _) =>
          raf.close
          filesR = filesR - name
      }
  }

  private def getOrOpenRW(name: String): RandomAccessFile = {
    filesRW.get(name).getOrElse(openWithCloserRW(name))._1
  }

  private def getOrOpenR(name: String): Try[RandomAccessFile] = {
    filesR.get(name).map(Success(_)).getOrElse(openWithCloserR(name)) map (_._1)
  }

  private def mkFile(name: String): File = {
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

  private def openWithCloserRW(name: String): FileWithCloser = {
    val file = mkFile(name)
    file.getParentFile.mkdirs()

    (
      new RandomAccessFile(file, "rw"),
      scheduleCloseRW(name)
    )
  }

  private def openWithCloserR(name: String): Try[FileWithCloser] = {
    val file = mkFile(name)

    Try {
      (
        new RandomAccessFile(file, "r"),
        scheduleCloseR(name)
      )
    }
  }

  private def read(raf: RandomAccessFile, offset: Int, length: Int): Array[Byte] = {
    @annotation.tailrec
    def doRead(length: Int, prevResult: Array[Byte]): Array[Byte] = {
      val ba = new Array[Byte](length)
      val bytesRead = raf.read(ba)

      val result = prevResult ++ ba.take(bytesRead)

      if (bytesRead == length) {
        result
      } else {
        doRead(length - bytesRead, result)
      }
    }

    doRead(length, new Array[Byte](0))
  }

  private def rescheduleCloseRW(name: String): Unit = {
    filesRW.get(name) map {
      case (raf, closer) =>
        closer.cancel()
        filesRW = filesRW + Tuple2(
          name,
          (
            raf,
            scheduleCloseRW(name)
          )
        )
    }
  }

  private def rescheduleCloseR(name: String): Unit = {
    filesR.get(name) map {
      case (raf, closer) =>
        closer.cancel()
        filesR = filesR + Tuple2(
          name,
          (
            raf,
            scheduleCloseR(name)
          )
        )
    }
  }

  private def scheduleCloseRW(name: String): Cancellable = {
    context.system.scheduler.scheduleOnce(closeTimeout, self, CloseRW(name))
  }

  private def scheduleCloseR(name: String): Cancellable = {
    context.system.scheduler.scheduleOnce(closeTimeout, self, CloseR(name))
  }
}
