package com.secretapp.backend.data.json.message.rpc.contact

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.contact._
import play.api.libs.json._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize ContactToImport" in {
      val v = ContactToImport(1, 2)
      val j = Json.obj(
        "clientPhoneId" -> "1",
        "phoneNumber"   -> "2"
      )
      testToAndFromJson[ContactToImport](j, v)
    }

    "(de)serialize PublicKeyRequest" in {
      val v = PublicKeyRequest(1, 2, 3)
      val j = Json.obj(
        "uid"        -> 1,
        "accessHash" -> "2",
        "keyHash"    -> "3"
      )
      testToAndFromJson[PublicKeyRequest](j, v)
    }

  }

}
