package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.websudos.util.testing.AsyncAssertionsHelper._
import scodec.bits._
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scalaz._
import Scalaz._

class UserRecordSpec extends Specification with CassandraSpecification with NoConcurrentExecutionContext {
  "UserRecord" should {
    "insert/get User Entity" in {
      val entity = User.build(uid = 100,
        authId = 10L,
        publicKey = hex"ac1d".bits,
        accessSalt = "salt",
        phoneNumber = 79853867016L,
        name = "Wayne Brain",
        sex = Male)
      val insertFuture = UserRecord.insertEntityWithPhoneAndPK(entity)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- UserRecord.getEntity(entity.uid)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
