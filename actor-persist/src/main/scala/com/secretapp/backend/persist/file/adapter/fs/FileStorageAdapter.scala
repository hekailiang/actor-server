package im.actor.server.persist.file.adapter.fs

import akka.actor._
import akka.util.{ ByteString, Timeout }
import akka.pattern.ask
import java.io.File
import java.net.URI
import java.nio.file.{ Path, Paths }
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import im.actor.server.persist.file.adapter.FileAdapter
import play.api.libs.iteratee.{ Enumerator, Iteratee }
import scala.concurrent._, duration._

object FileStorageAdapter {
  private lazy val md = MessageDigest.getInstance("md5")

  def mkBasePath(basePathStr: String): Path = {
    val rootURI = new URI("file:///")
    Paths.get(rootURI).resolve(basePathStr)
  }

  def mkFile(name: String, basePath: Path, pathDepth: Int): File = {
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

class FileStorageAdapter(system: ActorSystem, actorName: String = "fs-actor") extends FileAdapter {
  import FileStorageProtocol._

  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(5.seconds)

  private val fsConfig = system.settings.config.getConfig("file-storage")

  private val closeTimeout = fsConfig.getDuration("close-timeout", TimeUnit.MILLISECONDS).milliseconds
  private val basePathStr = fsConfig.getString("base-path")
  private val pathDepth = fsConfig.getInt("path-depth")


  private val basePath = FileStorageAdapter.mkBasePath(basePathStr)
  basePath.toFile.mkdir()

  val fsActor = system.actorOf(FileStorageActor.props(
    closeTimeout = closeTimeout,
    basePath = basePath,
    pathDepth = pathDepth
  ), actorName)

  def create(name: String): Future[Unit] = {
    Future.successful(())
  }

  def write(name: String, offset: Int, bytes: Array[Byte]): Future[Unit] = {
    Future {
      fsActor ! Write(name, offset, ByteString(bytes))
    }
  }

  def read(name: String, offset: Int, length: Int): Future[Array[Byte]] = {
    fsActor.ask(Read(name, offset, length)).mapTo[ReadBytes] map (_.data.toArray)
  }

  def read(name: String): Enumerator[Array[Byte]] = {
    Enumerator.fromFile(
      FileStorageAdapter.mkFile(name, basePath, pathDepth),
      1024 * 8
    )
  }

  protected def concat: Iteratee[Array[Byte], Array[Byte]] = {
    Iteratee.fold[Array[Byte], Array[Byte]](new Array[Byte](0)) {
      (res, data) =>
      res ++ data
    }
  }

  def readAll(name: String): Future[Array[Byte]] = {
    //Iteratee.flatten(read(name) |>> concat)
    read(name)(concat) flatMap (_.run)
  }
}
