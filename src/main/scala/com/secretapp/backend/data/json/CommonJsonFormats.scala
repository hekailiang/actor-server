package com.secretapp.backend.data.json

import play.api.data.validation.ValidationError
import play.api.libs.json._
import scodec.bits.BitVector
import scalaz._
import Scalaz._
import com.secretapp.backend.models

trait CommonJsonFormats {

  implicit object longFormat extends Format[Long] {
    override def writes(o: Long): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[Long] = json match {
      case JsNumber(n) => JsSuccess(n.toLong)
      case JsString(n) if (n.head.isDigit || n.head == '-') && n.tail.forall(_.isDigit) => JsSuccess(n.toLong)
      case _           => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }

  implicit object bitVectorFormat extends Format[BitVector] {
    private def strToBitVector(s: String): JsResult[BitVector] =
      BitVector.fromBase64(s) some (x => JsSuccess(x): JsResult[BitVector]) none JsError("error.expected.jsstring.base64")

    override def reads(json: JsValue): JsResult[BitVector] = json match {
      case JsString(s) => strToBitVector(s)
      case _           => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring.base64"))))
    }

    override def writes(o: BitVector): JsValue = JsString(o.toBase64)
  }

  // TODO: Move to the more appropriate place
  implicit object sexFormat extends Format[models.Sex] {
    override def reads(json: JsValue): JsResult[models.Sex] = json match {
      case JsString("male")   => JsSuccess(models.Male)
      case JsString("female") => JsSuccess(models.Female)
      case JsString("nosex")  => JsSuccess(models.NoSex)
      case _                  => JsError()
    }

    override def writes(o: models.Sex): JsValue = o match {
      case models.Male   => JsString("male")
      case models.Female => JsString("female")
      case models.NoSex  => JsString("nosex")
    }
  }

  // TODO: Move to the more appropriate place
  // FIXME: Looks like macro bug.
  val fileLocationFormat: Format[models.FileLocation] = Json.format[models.FileLocation]
  implicit val implicitfileLocationFormat = fileLocationFormat

  // TODO: Move to the more appropriate place
  implicit val avatarImageFormat = Json.format[models.AvatarImage]

  // TODO: Move to the more appropriate place
  // FIXME: Looks like macro bug.
  val avatarFormat: Format[models.Avatar] = Json.format[models.Avatar]
  implicit val implicitAvatarFormat = avatarFormat

}

object CommonJsonFormats extends CommonJsonFormats {

}
