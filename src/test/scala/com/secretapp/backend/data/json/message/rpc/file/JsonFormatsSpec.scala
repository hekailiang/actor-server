package com.secretapp.backend.data.json.message.rpc.file

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.file._
import play.api.libs.json._
import JsonFormatsSpec._
import scala.util.Random

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize UploadConfig" in {
      val v = UploadConfig(bitvector)
      val j = Json.obj(
        "serverData" -> bitvectorJson
      )
      testToAndFromJson[UploadConfig](j, v)
    }

    "(de)serialize FileLocation" in {
      val (v, j) = fileLocation
      testToAndFromJson[FileLocation](j, v)
    }

  }

}

object JsonFormatsSpec {

  def fileLocation = {
    val fileId = Random.nextLong()
    val accessHash = Random.nextLong()

    (
      FileLocation(fileId, accessHash),
      Json.obj(
        "fileId" -> fileId.toString,
        "accessHash" -> accessHash.toString
      )
    )
  }

}
