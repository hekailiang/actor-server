package json

import play.api.data.validation.ValidationError
import play.api.libs.json._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object CommonJsonFormats {

  implicit object bitVectorFormat extends Format[BitVector] {

    private def strToBitVector(s: String): JsResult[BitVector] =
      BitVector.fromBase64(s)
        .map(JsSuccess(_))
        .getOrElse(JsError("error.expected.jsstring.base64"))

    override def reads(json: JsValue): JsResult[BitVector] = json match {
      case JsString(s) => strToBitVector(s)
      case _           => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring.base64"))))
    }

    override def writes(o: BitVector): JsValue = JsString(o.toBase64)

  }

}
