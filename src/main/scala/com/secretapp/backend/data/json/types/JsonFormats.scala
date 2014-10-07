package com.secretapp.backend.data.json.types

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.types._
import play.api.libs.json._

trait JsonFormats {

  implicit val sexFormat = new Format[Sex] {
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

}
