package com.secretapp.backend.data.json.message.rpc.messaging

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.messaging._
import play.api.libs.json._

import scala.collection.immutable

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize EncryptedKey" in {
      testToAndFromJson[EncryptedKey](encKeyJson, encKey)
    }

    "(de)serialize EncryptedMessage" in {
      val v = EncryptedMessage(bitvector, immutable.Seq(encKey))
      val j = Json.obj(
        "message" -> bitvectorJson,
        "keys"    -> Json.arr(encKeyJson)
      )
      testToAndFromJson[EncryptedMessage](j, v)
    }

  }

  private def encKey = EncryptedKey(1, bitvector)
  private def encKeyJson = Json.obj(
    "keyHash"         -> "1",
    "aesEncryptedKey" -> bitvectorJson
  )
}

