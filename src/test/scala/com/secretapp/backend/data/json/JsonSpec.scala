package com.secretapp.backend.data.json

import org.specs2.mutable.Specification
import play.api.libs.json._
import scodec.bits.BitVector

trait JsonSpec extends Specification {

  protected def testToAndFromJson[A](json: JsValue, value: A)
                          (implicit f: Format[A]) = {
    Json.toJson(value)         should be_== (json)
    Json.fromJson[A](json).get should be_== (value)
  }

  protected val bitvector = BitVector.fromBase64("1234").get
  protected val bitvectorJson = JsString("1234")
}
