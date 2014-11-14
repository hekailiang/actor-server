package models.json

import models.CommonJsonFormats._
import play.api.libs.json._

object User extends Writes[models.User] {

  override def writes(u: models.User): JsValue =
    Json.obj(
      "id"            -> u.id,
      "authId"        -> u.authId,
      "publicKeyHash" -> u.publicKeyHash,
      "publicKey"     -> u.publicKey,
      "phoneNumber"   -> u.phoneNumber,
      "name"          -> u.name,
      "sex"           -> u.sex,
      "avatar"        -> u.avatar,
      "keyHashes"     -> u.keyHashes
    )

}
