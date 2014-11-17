package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import scodec.bits.BitVector
import scala.util.Random
import scala.collection.immutable
import models.json._

case class UserCreationRequest(
  publicKey:   BitVector,
  phoneNumber: Long,
  name:        String,
  sex:         Sex,
  avatar:      Option[Avatar] = None
) {

  def toUser: models.User = {
    val publicKeyHash = PublicKey.keyHash(publicKey)

    models.User(
      0, // Will be generated at persist-time anyway.
      Random.nextLong(),
      publicKeyHash,
      publicKey,
      phoneNumber,
      Random.nextString(30),
      name,
      sex,
      avatar,
      immutable.Set(publicKeyHash)
    )
  }

}

object UserCreationRequest {

  implicit val jsonFormat = Json.format[UserCreationRequest]

}
