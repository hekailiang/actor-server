package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import scodec.bits.BitVector
import utils.OptSet._
import scalaz._
import Scalaz._
import models.json._
import com.secretapp.backend.models.{Avatar, Sex}

case class UserUpdateRequest(
  publicKey:   Option[BitVector],
  phoneNumber: Option[Long],
  name:        Option[String],
  sex:         Option[Sex],
  avatar:      Option[Avatar]
) {

  def update(u: models.User): models.User =
    u
      .optSet(publicKey  )((u, v) => u.copy(publicKey   = v))
      .optSet(phoneNumber)((u, v) => u.copy(phoneNumber = v))
      .optSet(name       )((u, v) => u.copy(name        = v))
      .optSet(sex        )((u, v) => u.copy(sex         = v))
      .optSet(avatar     )((u, v) => u.copy(avatar      = v.some))

}

object UserUpdateRequest {

  implicit val jsonFormat = Json.format[UserUpdateRequest]

}
