package com.secretapp.backend.persist

import scala.collection.immutable.Seq
import com.websudos.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.types._
import com.secretapp.backend.data.models._
import scodec.bits._
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import test.utils.specifications.RandSpecification
import scalaz._
import Scalaz._

class SessionIdRecordSpec extends Specification with CassandraSpecification with NoConcurrentExecutionContext
with RandSpecification {
  "SessionId" should {
    "insert/get SessionId item" in {
      val authId = rand.nextLong
      val sessionId = rand.nextLong
      val item = SessionId(authId, sessionId)
      val insertFuture = SessionIdRecord.insertEntity(item)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- SessionIdRecord.getEntity(authId, sessionId)
      } yield oneSelect

      chain must be_== (item.some).await
    }
  }
}
