package models

import play.api.libs.json.Json

object AuthSmsCode {

  implicit val jsonWrites = json.AuthSmsCode

}
