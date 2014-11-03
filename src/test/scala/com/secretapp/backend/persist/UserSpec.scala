package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import scodec.bits._
import com.secretapp.backend.models
import com.secretapp.backend.crypto.ec
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scala.collection.immutable
import scalaz._
import Scalaz._

class UserSpec extends Specification with CassandraSpecification with NoConcurrentExecutionContext {
  "UserRecord" should {
    "insert/get User Entity" in {
      val publicKey = hex"ac1d".bits
      val pkHash = ec.PublicKey.keyHash(publicKey)
      val entity = models.User(
        100,
        10L,
        pkHash,
        publicKey,
        79853867016L,
        "salt",
        "Wayne Brain",
        models.Male,
        keyHashes = immutable.Set(pkHash))
      val insertFuture = User.insertEntityWithChildren(entity)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- User.getEntity(entity.uid)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
