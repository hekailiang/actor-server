package com.secretapp.backend.persist

import com.secretapp.backend.services.GeneratorService
import scodec.bits._
import com.secretapp.backend.models
import com.secretapp.backend.crypto.ec
import im.actor.server.persist.unit.SqlSpec
import im.actor.testkit.ActorSpecification
import org.specs2.mutable.Specification
import org.specs2.matcher.NoConcurrentExecutionContext
import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

class UserSpec extends ActorSpecification with SqlSpec with GeneratorService with NoConcurrentExecutionContext {
  "UserRecord" should {
    "insert/get User Entity" in new sqlDb {
      val publicKey = hex"ac1d".bits
      val pkHash = ec.PublicKey.keyHash(publicKey)
      val userId = rand.nextInt
      val phoneId = rand.nextInt

      val phoneNumber = 79817796093L

      val entity = models.User(
        userId,
        10L,
        pkHash,
        publicKey,
        phoneNumber,
        "salt",
        "Wayne Brain",
        "RU",
        models.Male,
        publicKeyHashes = immutable.Set(pkHash),
        phoneIds = immutable.Set(phoneId),
        emailIds = immutable.Set.empty,
        state = models.UserState.Registered
      )

      val insertPhoneFuture = UserPhone.create(
        id = phoneId,
        userId = userId,
        accessSalt = "phone_salt",
        number = phoneNumber,
        title = "Mobile phone"
      )

      val insertUserFuture = User.create(
        id = entity.uid,
        accessSalt = entity.accessSalt,
        name = entity.name,
        countryCode = entity.countryCode,
        sex = entity.sex,
        state = entity.state
      )(
        authId = entity.authId,
        publicKeyHash = entity.publicKeyHash,
        publicKeyData = entity.publicKeyData
      )

      val chain = for {
        insertDone <- Future.sequence(Seq(insertPhoneFuture, insertUserFuture))
        oneSelect <- User.find(entity.uid)(None)
      } yield oneSelect

      chain must be_== (entity.some).await
    }
  }
}
