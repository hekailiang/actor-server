package com.secretapp.backend.persist

import scala.collection.immutable.Seq
import com.newzly.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.types._
import com.secretapp.backend.data.models._
import scodec.bits._
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scalaz._
import Scalaz._

class UserRecordSpec extends Specification with CassandraSpecification with NoConcurrentExecutionContext {
  "UserRecord" should {
    "insert/get User Entity" in {
      val entityId = 100
      val entity = User(entityId, 123L, hex"ac1d".bits, "Wayne", "salt", Some("Brain"), Male, Seq(123L))
      val insertFuture = UserRecord.insertEntity(entity)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- UserRecord.getEntity(entityId)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
