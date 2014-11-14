package models

import play.api.libs.json.Json

case class AuthSmsCode(phone: Long, smsHash: String, smsCode: String)

object AuthSmsCode {

  implicit val jsonWrites = json.AuthSmsCode

}
