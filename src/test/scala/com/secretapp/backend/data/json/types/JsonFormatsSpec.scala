package com.secretapp.backend.data.json.types

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.types._
import play.api.libs.json._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize Sex" in {
      Json.toJson(Male)                        should be_== (JsString("male"))
      Json.fromJson[Sex](JsString("male")).get should be_== (Male)

      Json.toJson(Female)                        should be_== (JsString("female"))
      Json.fromJson[Sex](JsString("female")).get should be_== (Female)

      Json.toJson(NoSex)                        should be_== (JsString("nosex"))
      Json.fromJson[Sex](JsString("nosex")).get should be_== (NoSex)
    }

  }

}
