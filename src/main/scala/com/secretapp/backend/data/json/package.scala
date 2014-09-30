package com.secretapp.backend.data

import play.api.data.validation.ValidationError
import play.api.libs.json._

package object json {
  implicit object longFormat extends Format[Long] {
    override def writes(o: Long): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[Long] = json match {
      case JsNumber(n) => JsSuccess(n.toLong)
      case JsString(n) if n.forall(_.isDigit) => JsSuccess(n.toLong)
      case _           => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }
}
