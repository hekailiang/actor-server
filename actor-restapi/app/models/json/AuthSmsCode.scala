package models.json

import models.CommonJsonFormats._
import play.api.libs.json._
import com.secretapp.backend.models

object AuthSmsCode extends Writes[models.AuthSmsCode] {

  override def writes(c: models.AuthSmsCode): JsValue =
    Json.obj(
      "phoneNumber" -> c.phoneNumber,
      "smsCode"     -> c.smsCode,
      "smsHash"     -> c.smsHash
    )

}
