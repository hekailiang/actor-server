package com.secretapp.backend.data.json.types

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.models
import play.api.libs.json._

// TODO: Move away from .types ns, it does not exists
class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize Sex" in {
      Json.toJson(models.Male)                        should be_== (JsString("male"))
      Json.fromJson[models.Sex](JsString("male")).get should be_== (models.Male)

      Json.toJson(models.Female)                        should be_== (JsString("female"))
      Json.fromJson[models.Sex](JsString("female")).get should be_== (models.Female)

      Json.toJson(models.NoSex)                        should be_== (JsString("nosex"))
      Json.fromJson[models.Sex](JsString("nosex")).get should be_== (models.NoSex)
    }

  }

}
