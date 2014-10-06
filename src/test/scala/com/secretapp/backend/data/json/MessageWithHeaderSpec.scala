package com.secretapp.backend.data.json

import com.secretapp.backend.data.json.MessageWithHeader._
import org.specs2.mutable.Specification
import play.api.libs.json._

class MessageWithHeaderSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize MessageWithHeader" in {
      val v = MessageWithHeader(1, Json.obj("something" -> "here"))
      val j = Json.obj(
        "header" -> 1,
        "body" -> Json.obj(
          "something" -> "here"
        )
      )
      testToAndFromJson(j, v)
    }
  }
}
