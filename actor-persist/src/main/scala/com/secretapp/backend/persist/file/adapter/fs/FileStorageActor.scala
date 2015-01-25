package im.actor.server.persist.file.adapter.fs

import akka.actor._
import akka.util.ByteString
import java.io.RandomAccessFile
import java.io.File
import java.nio.file.{ Files, Path }
import im.actor.util.logging.FailureReplyingActor
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
  case class ReadBytes(name: String, data: ByteString) extends Message

  @SerialVersionUID(1L)
  case object FileNotExists extends Message
}

object FileStorageActor {
  def props(closeTimeout: FiniteDuration, basePath: Path, pathDepth: Int): Props = {
    val maxPathDepth = 15

    if (pathDepth > maxPathDepth)
      throw new Error(s"PathDepth can not be more than $maxPathDepth")

    Props(classOf[FileStorageActor], closeTimeout, basePath, pathDepth)
  }
}

class FileStorageActor(closeTimeout: FiniteDuration, basePath: Path, pathDepth: Int) extends FailureReplyingActor with ActorLogging {
  import context.dispatcher
  import FileStorageProtocol._

  type FileWithCloser = (RandomAccessFile, Cancellable)

  private var filesRW: immutable.Map[String, FileWithCloser] = immutable.Map.empty
  private var filesR: immutable.Map[String, FileWithCloser] = immutable.Map.empty

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    closeFiles()
  }

  def receive = {
    case Write(name, offset, bytes) =>
      val replyTo = sender

      Try {
        log.debug("Writing chunk name: {}, offset: {}, length: {}", name, offset, bytes.length)
        val raf = getOrOpenRW(name)

        val requiredLength = offset + bytes.length

        if (raf.length < requiredLength)
          raf.setLength(requiredLength)

        raf.seek(offset)
        raf.write(bytes.toArray)

        cacheAndScheduleCloseRW(name, raf)

      } match {
        case Success(_) => sender ! Wrote
        case Failure(e) =>
          sender ! Status.Failure(e)
          throw e
      }
    case Read(name, offset, length) =>
      val replyTo = sender
      log.debug("Requested read name: {}, offset: {}, length: {}", name, offset, length)

      getOrOpenR(name) match {
        case Success(raf) =>
          log.debug("Reading name: {}, offset: {}, length: {}", name, offset, length)
          val bytes = read(raf, offset, length)

          log.debug("Chunk read name: {}, offset: {}, requested length: {}, real length: {}",
            name, offset, length, bytes.length)
          replyTo ! ReadBytes(name, ByteString(bytes))

          cacheAndScheduleCloseR(name, raf)
        case Failure(_) =>
          replyTo ! FileNotExists
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
    filesRW.get(name) map {
      case (raf, _) =>
        log.debug("Handler(rw) got from cache {}", name)
        raf
    } getOrElse {
      log.debug("Opening handler(rw) {}", name)
      openRW(name)
    }
  }

  private def getOrOpenR(name: String): Try[RandomAccessFile] = {
    filesR.get(name) map {
      case (raf, _) =>
        log.debug("Handler(r) got from cache {}", name)
        Success(raf)
    } getOrElse {
      log.debug("Opening handler(r) {}", name)
      openR(name)
    }
  }

  private def openRW(name: String): RandomAccessFile = {
    val file = FileStorageAdapter.mkFile(name, basePath, pathDepth)
    file.getParentFile.mkdirs()

    log.debug("Real file path: {}, name: {}", file.toPath().toString(), name)

    new RandomAccessFile(file, "rw")
  }

  private def openR(name: String): Try[RandomAccessFile] = {
    Try {
      val file = FileStorageAdapter.mkFile(name, basePath, pathDepth)

      log.debug("Real file path: {}, name: {}", file.toPath().toString(), name)

      new RandomAccessFile(file, "r")
    }
  }

  private def read(raf: RandomAccessFile, offset: Int, length: Int): Array[Byte] = {
    @annotation.tailrec
    def doRead(result: Array[Byte]): Array[Byte] = {
      Try (raf.readByte()) match {
        case Success(byte) =>
          val newResult = result :+ byte

          if (newResult.length == length)
            newResult
          else
            doRead(newResult)
        case Failure(e: java.io.EOFException) => result
        case Failure(e) => throw e
      }
    }

    raf.seek(offset)
    doRead(Array[Byte]())
  }

  /*
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
   */

  private def scheduleCloseRW(name: String): Cancellable = {
    context.system.scheduler.scheduleOnce(closeTimeout, self, CloseRW(name))
  }

  private def scheduleCloseR(name: String): Cancellable = {
    context.system.scheduler.scheduleOnce(closeTimeout, self, CloseR(name))
  }

  private def cacheAndScheduleCloseRW(name: String, raf: RandomAccessFile): Unit = {
    filesRW.get(name).map(_._2.cancel())
    filesRW = filesRW + Tuple2(
      name,
      (raf, scheduleCloseRW(name))
    )
  }

  private def cacheAndScheduleCloseR(name: String, raf: RandomAccessFile): Unit = {
    filesR.get(name).map(_._2.cancel())
    filesR = filesR + Tuple2(
      name,
      (raf, scheduleCloseR(name))
    )
  }

  private def closeFiles(): Unit = {
    filesR foreach {
      case (_, (raf, closer)) =>
        closer.cancel()
        raf.close()
    }

    filesRW foreach {
      case (_, (raf, closer)) =>
        closer.cancel()
        raf.close()
    }
  }
}
