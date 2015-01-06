package com.secretapp.backend.persist

import com.secretapp.backend.services.GeneratorService
import com.websudos.phantom.Implicits._
import scodec.bits._
import com.secretapp.backend.models
import com.secretapp.backend.crypto.ec
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scala.collection.immutable
import scalaz._
import Scalaz._

class UserSpec extends Specification with CassandraSpecification with GeneratorService with NoConcurrentExecutionContext {
  "UserRecord" should {
    "insert/get User Entity" in {
      val publicKey = hex"ac1d".bits
      val pkHash = ec.PublicKey.keyHash(publicKey)
      val userId = 100
      val phoneId = rand.nextInt
      val phone = models.UserPhone(rand.nextInt, userId, "phone_salt", 79817796093L, "Mobile phone")
      val entity = models.User(
        userId,
        10L,
        pkHash,
        publicKey,
        79853867016L,
        "salt",
        "Wayne Brain",
        "RU",
        models.Male,
        keyHashes = immutable.Set(pkHash),
        phoneIds = immutable.Set(phoneId),
        emailIds = immutable.Set.empty,
        state = models.UserState.Registered
      )
      val insertFuture = User.insertEntityWithChildren(entity, models.AvatarData.empty)

      val chain = for {
        insertDone <- insertFuture
        oneSelect <- User.getEntity(entity.uid)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
