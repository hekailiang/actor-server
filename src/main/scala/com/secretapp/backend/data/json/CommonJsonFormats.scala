package com.secretapp.backend.data.json

import play.api.data.validation.ValidationError
import play.api.libs.json._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

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
}

object CommonJsonFormats extends CommonJsonFormats {

}
