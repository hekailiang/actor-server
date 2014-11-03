package com.secretapp.backend.persist

import akka.util.Timeout
import com.datastax.driver.core.ConsistencyLevel
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.protocol.codecs.common.StringCodec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import com.websudos.util.testing.AsyncAssertionsHelper._

class FileSpec extends CassandraSpecification {
  "FileRecord" should {
    "insert and get file" in {
      val Record = new File

      val fileId = 1
      val content = ((1 to (1024 * 20)) map (i => (i % 255).toByte)).toArray
      val fileSize = content.length

      val fResult = Record.write(fileId, 0, content) map { _ =>

        val (fv1, fv2, fv3, fv4, fv5, fv6, feverything1, feverything2) = (
          Record.getFile(fileId, 0, 1024),
          Record.getFile(fileId, 0, 8192),
          Record.getFile(fileId, 0, 9216), // more than a blocksize
          Record.getFile(fileId, 1024, 1024),
          Record.getFile(fileId, 1024, 9216),
          Record.getFile(fileId, fileSize - 1024, 1024),
          Record.getFile(fileId, 0, fileSize),
          Record.getFile(fileId)
        )

        val f = for {
          v1 <- fv1
          v2 <- fv2
          v3 <- fv3
          v4 <- fv4
          v5 <- fv5
          v6 <- fv6
          everything1 <- feverything1
          everything2 <- feverything2
        } yield {
          v1 must equalTo(content.take(1024))
          v2 must equalTo(content.take(8192))
          v3 must equalTo(content.take(9216))
          v4.length must equalTo(content.drop(1024).take(1024).length)
          v4 must equalTo(content.drop(1024).take(1024))
          v5 must equalTo(content.drop(1024).take(9216))
          v6 must equalTo(content.drop(fileSize - 1024))
          everything1 must equalTo(content)
          everything2 must equalTo(content)
        }

        f.await(30)
      }

      fResult.await(30)
    }
  }
}
