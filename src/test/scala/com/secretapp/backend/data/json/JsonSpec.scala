package com.secretapp.backend.data.json

import org.specs2.mutable.Specification
import play.api.libs.json._
import scodec.bits.BitVector

import scala.util.Random

trait JsonSpec extends Specification {

  protected def testToAndFromJson[A](json: JsValue, value: A)
                          (implicit f: Format[A]) = {
    Json.toJson(value)         should be_== (json)
    Json.fromJson[A](json).get should be_== (value)
  }

  protected def withHeader(header: Int)(body: (String, Json.JsValueWrapper)*) =
    Json.obj(
      "header" -> header,
      "body" -> Json.obj(body: _*)
    )

}

object JsonSpec {

  def genBitVector = {
    val content = "%04d".format(Math.abs(Random.nextInt()) % 10000)
    (
      BitVector.fromBase64(content).get,
      JsString(content)
    )
  }

}
