package com.secretapp.backend.persist

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import scala.collection.immutable.Seq
import com.newzly.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.types._
import com.secretapp.backend.data.models._
import scodec.bits._

class UserRecordSpec extends CassandraFlatSpec {
  implicit val s: PatienceConfiguration.Timeout = timeout(10 seconds)

  "UserRecord" should "insert/get User Entity" in {
    val entityId = 100
    val entity = Entity(entityId, User(123L, hex"ac1d".bits, "Wayne", "salt", Some("Brain"), Male, Seq(123L)))
    val insertFuture = UserRecord.insertEntity(entity)

    val chain = for {
      insertDone <- insertFuture
      oneSelect <- UserRecord.getEntity(entityId)
    } yield (oneSelect)

    chain successful {
      resultOpt => {
        resultOpt.get shouldEqual entity
      }
    }
  }
}
