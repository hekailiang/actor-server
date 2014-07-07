package com.secretapp.backend.persist

import scala.concurrent.blocking
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.Implicits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data._

class UserRecordSpec extends CassandraSpec {
  val keySpace: String = "secret"
  implicit val s: PatienceConfiguration.Timeout = timeout(10 seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()

    blocking {
      UserRecord.createTable(session).sync()
    }
  }

  "UserRecord" should "insert/get User Entity" in {
    val entity = Entity(100L, User("Wayne", "Brain", Male))
    val insertFuture = UserRecord.insertEntity(entity)

    val chain = for {
      insertDone <- insertFuture
      oneSelect <- UserRecord.getEntity(100L)
    } yield (oneSelect)

    chain successful {
      resultOpt => {
        resultOpt.get shouldEqual entity
      }
    }
  }
}
