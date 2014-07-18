package com.secretapp.backend.persist

import com.newzly.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.models._
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scalaz._
import Scalaz._

class AuthIdRecordSpecification extends Specification with CassandraSpecification with NoConcurrentExecutionContext {
  "AuthIdRecord" should {
    "insert/get AuthId Entity" in {
      val entity = AuthId(123L, None)
      val insertFuture = AuthIdRecord.insertEntity(entity)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- AuthIdRecord.getEntity(entity.authId)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
