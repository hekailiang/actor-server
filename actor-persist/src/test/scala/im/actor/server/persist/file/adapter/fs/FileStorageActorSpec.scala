package im.actor.server.persist.file.adapter.fs

import akka.actor._
import akka.testkit._
import akka.util.ByteString
import com.typesafe.config._
import im.actor.testkit.ActorSpecification
import java.io.{ File, FileNotFoundException, RandomAccessFile }
import java.util.concurrent.TimeUnit
import org.specs2.mutable
import scala.concurrent.duration._

class FileStorageActorSpec extends ActorSpecification(
  ActorSystem(
    "actor-server-test",
    ConfigFactory.parseString("""
       actor-server {
         file-storage {
           base-path = /tmp
           close-timeout = 3 seconds
           path-depth = 5
         }
       }
    """)
      .withFallback(ConfigFactory.load())
      .getConfig("actor-server")
  )
) {
  import FileStorageProtocol._

  val probe = TestProbe()

  val fsConfig = system.settings.config.getConfig("file-storage")

  val fsActor = system.actorOf(FileStorageActor.props(
    closeTimeout = fsConfig.getDuration("close-timeout", TimeUnit.MILLISECONDS).milliseconds,
    basePathStr = fsConfig.getString("base-path"),
    pathDepth = fsConfig.getInt("path-depth")
  ), "fs")

  lazy val fileName = "actor-magick"
  lazy val filePath = "/tmp/0b/fa/5e/ed/56/00debe056c024ff175ef77"

  trait cleaner extends mutable.After with mutable.Before {
    def before = removeFile()

    def after = removeFile()

    def removeFile() = {
      val file = new File(filePath)
      file.delete()
    }
  }

  "FileStorageActor" should {

    "create file on write" in new cleaner {
      fsActor ! Write(fileName, 0, ByteString("one two"))
      fsActor ! Write(fileName, 7, ByteString(" three".getBytes))
      expectMsg(1.second, Wrote)
      expectMsg(1.second, Wrote)

      new RandomAccessFile(new File(filePath), "r")
    }

    "not create file on Read and reply with FileNotExists" in new cleaner {
      fsActor ! Read(fileName, 0, 5)
      expectMsg(1.second, FileNotExists)

      {
        new RandomAccessFile(new File(filePath), "r")
      } must throwA[FileNotFoundException]
    }

    "write bytes in proper order" in new cleaner {
      // Let closer to close fileopened in prev spec and removed by cleaner
      Thread.sleep(3500)

      fsActor ! Write(fileName, 10, ByteString("one two three"))
      fsActor ! Write(fileName, 0, ByteString("0123456789"))

      receiveN(2)

      fsActor ! Read(fileName, 9, 4)

      expectMsg(ReadBytes(fileName, ByteString("9one")))
    }
  }
}
