package com.secretapp.backend.data.json.transport

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.json.JsonSpec._
import play.api.libs.json.Json

import scala.util.Random

class JsonFormatsSpec extends JsonSpec {

  import JsonFormatsSpec._

  "(de)serializer" should {

    "(de)serialize JsonPackage" in {
      val (messageBoxBytes, messageBoxBytesJson) = genBitVector
      val v = JsonPackage(1, 2, messageBoxBytes)
      val j = Json.obj(
        "authId" -> "1",
        "sessionId" -> "2",
        "messageBoxBytes" -> messageBoxBytesJson
      )
      testToAndFromJson(j, v)
    }

    "(de)serialize MTPackage" in {
      val (v, j) = genMTPackage
      testToAndFromJson(j, v)
    }

    "(de)serialize MTPackageBox" in {
      val (p, pJson) = genMTPackage
      val v = MTPackageBox(1, p)
      val j = Json.obj(
        "index" -> 1,
        "p" -> pJson
      )
      testToAndFromJson(j, v)
    }

  }

}

object JsonFormatsSpec {

  def genMTPackage = {
    val (messageBoxBytes, messageBoxBytesJson) = genBitVector
    val authId = Random.nextLong()
    val sessionId = Random.nextLong()

    (
      MTPackage(authId, sessionId, messageBoxBytes),
      Json.obj(
        "authId" -> authId.toString,
        "sessionId" -> sessionId.toString,
        "messageBoxBytes" -> messageBoxBytesJson
      )
    )
  }
}
