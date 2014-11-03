package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scalaz._
import Scalaz._

class AuthIdSpec extends Specification with CassandraSpecification with NoConcurrentExecutionContext {
  "AuthIdRecord" should {
    "insert/get AuthId Entity" in {
      val entity = models.AuthId(123L, None)
      val insertFuture = AuthId.insertEntity(entity)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- AuthId.getEntity(entity.authId)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
