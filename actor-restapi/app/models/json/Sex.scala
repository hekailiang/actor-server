package models.json

import models.CommonJsonFormats._
import play.api.libs.json._

object Sex extends Format[models.Sex] {

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
