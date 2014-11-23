import json.CommonJsonFormats._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.secretapp.backend.models._

package object json {

  implicit object authSmsCodeJsonWrites extends Writes[AuthSmsCode] {
    override def writes(c: AuthSmsCode): JsValue =
      Json.obj(
        "phoneNumber" -> c.phoneNumber,
        "smsCode"     -> c.smsCode,
        "smsHash"     -> c.smsHash
      )
  }

  implicit val fileLocationJsonFormat = Json.format[FileLocation]

  implicit val avatarImageJsonFormat = Json.format[AvatarImage]

  implicit val avatarJsonFormat: Format[Avatar] = (
    (JsPath \ "smallImage").format[Option[AvatarImage]] ~
    (JsPath \ "largeImage").format[Option[AvatarImage]] ~
    (JsPath \ "fullImage" ).format[Option[AvatarImage]]
  )(Avatar.apply, unlift(Avatar.unapply))

  implicit object sexJsonFormat extends Format[Sex] {
    override def reads(json: JsValue): JsResult[Sex] = json match {
      case JsString("male")   => JsSuccess(Male)
      case JsString("female") => JsSuccess(Female)
      case JsString("nosex")  => JsSuccess(NoSex)
      case _                  => JsError()
    }

    override def writes(o: Sex): JsValue = o match {
      case Male   => JsString("male")
      case Female => JsString("female")
      case NoSex  => JsString("nosex")
    }
  }

  implicit object userJsonWrites extends Writes[User] {
    override def writes(u: User): JsValue =
      Json.obj(
        "id"            -> u.uid,
        "authId"        -> u.authId,
        "publicKeyHash" -> u.publicKeyHash,
        "publicKey"     -> u.publicKey,
        "phoneNumber"   -> u.phoneNumber,
        "name"          -> u.name,
        "countryCode"   -> u.countryCode,
        "sex"           -> u.sex,
        "avatar"        -> u.avatar,
        "keyHashes"     -> u.keyHashes
      )
  }

  implicit val authSmsCodeCreationRequestJsonFormat = Json.format[AuthSmsCodeCreationRequest]

  implicit val authSmsCodeUpdateRequestJsonFormat = Json.format[AuthSmsCodeUpdateRequest]

  implicit val errorJsonFormat = Json.format[Error]

  implicit val userCreationRequestJsonFormat = Json.format[UserCreationRequest]

  implicit val userUpdateRequestJsonFormat = Json.format[UserUpdateRequest]

}
