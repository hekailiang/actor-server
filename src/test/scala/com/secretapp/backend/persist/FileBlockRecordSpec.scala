package com.secretapp.backend.persist

import akka.util.Timeout
import com.datastax.driver.core.ConsistencyLevel
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.protocol.codecs.common.StringCodec
import scala.collection.immutable.Seq
import scala.concurrent.{ Await }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import com.newzly.util.testing.AsyncAssertionsHelper._

class FileBlockRecordSpec extends CassandraSpecification {
  "FileBlockRecord" should {
    "insert and get file" in {
      val Record = new FileBlockRecord

      val fileId = 1
      val content = ((1 to (1024 * 1024)) map (i => (i % 255).toByte)).toArray
      val fileSize = content.length

      val f = for {
        _ <- Record.write(fileId, 0, content)
        v1 <- Record.getFile(fileId, 0, 1024)
        v2 <- Record.getFile(fileId, 0, 9216) // more than a blocksize
        v3 <- Record.getFile(fileId, 1024, 1024)
        v4 <- Record.getFile(fileId, 1024, 9216)
        v5 <- Record.getFile(fileId, fileSize - 1024, 1024)
        everything <- Record.getFile(fileId, 0, fileSize)
      } yield {
        v1 must equalTo(content.take(1024))
        v2 must equalTo(content.take(9216))
        v3.length must equalTo(content.drop(1024).take(1024).length)
        v3 must equalTo(content.drop(1024).take(1024))
        v4 must equalTo(content.drop(1024).take(9216))
        v5 must equalTo(content.drop(fileSize - 1024))
        everything must equalTo(content)
      }

      f.await(10)
    }
  }
}
