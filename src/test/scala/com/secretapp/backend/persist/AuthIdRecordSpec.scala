package com.secretapp.backend.persist

import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.models._

class AuthIdRecordSpec extends CassandraFlatSpec {
  implicit val s: PatienceConfiguration.Timeout = timeout(10 seconds)

  "AuthIdRecord" should "insert/get AuthId Entity" in {
    val entity = AuthId(123L, None)
    val insertFuture = AuthIdRecord.insertEntity(entity)

    val chain = for {
      insertDone <- insertFuture
      oneSelect <- AuthIdRecord.getEntity(entity.authId)
    } yield (oneSelect)

    chain successful {
      resultOpt => {
        resultOpt.get shouldEqual entity
      }
    }
  }
}
