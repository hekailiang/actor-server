package models.json

import models.CommonJsonFormats._
import play.api.libs.json._

object AuthSmsCode extends Writes[models.AuthSmsCode] {

  override def writes(c: models.AuthSmsCode): JsValue =
    Json.obj(
      "phoneNumber" -> c.phone,
      "smsCode"     -> c.smsCode,
      "smsHash"     -> c.smsHash
    )

}
