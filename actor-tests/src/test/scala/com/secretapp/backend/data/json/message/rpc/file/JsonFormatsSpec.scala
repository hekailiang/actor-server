package com.secretapp.backend.data.json.message.rpc.file

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc.{RpcResponseMessage, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.file._
import play.api.libs.json._
import JsonFormatsSpec._
import scala.util.Random

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize UploadConfig" in {
      val (v, j) = genUploadConfig
      testToAndFromJson(j, v)
    }

    "(de)serialize FileLocation" in {
      val (v, j) = genFileLocation
      testToAndFromJson(j, v)
    }

  }

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestCompleteUpload" in {
      val (uploadConfig, uploadConfigJson) = genUploadConfig
      val v = RequestCompleteUpload(uploadConfig, 1, 2)
      val j = withHeader(RequestCompleteUpload.header)(
        "config" -> uploadConfigJson,
        "blockCount" -> 1,
        "crc32" -> "2"
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetFile" in {
      val (fileLocation, fileLocationJson) = genFileLocation
      val v = RequestGetFile(fileLocation, 1, 2)
      val j = withHeader(RequestGetFile.header)(
        "fileLocation" -> fileLocationJson,
        "offset" -> 1,
        "limit" -> 2
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestStartUpload" in {
      val v = RequestStartUpload()
      val j = withHeader(RequestStartUpload.header)()
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestUploadPart" in {
      val (uploadConfig, uploadConfigJson) = genUploadConfig
      val (bitVector, bitVectorJson) = genBitVector
      val v = RequestUploadPart(uploadConfig, 1, bitVector)
      val j = withHeader(RequestUploadPart.header)(
        "config" -> uploadConfigJson,
        "offset" -> 1,
        "data" -> bitVectorJson
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

  "RpcResponseMessage (de)serializer" should {

    "(de)serialize ResponseFilePart" in {
      val (data, dataJson) = genBitVector
      val v = ResponseFilePart(data)
      val j = withHeader(ResponseFilePart.header)(
        "data" -> dataJson
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

    "(de)serialize ResponseUploadCompleted" in {
      val (fileLocation, fileLocationJson) = genFileLocation
      val v = ResponseUploadCompleted(fileLocation)
      val j = withHeader(ResponseUploadCompleted.header)(
        "location" -> fileLocationJson
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

    "(de)serialize ResponseUploadStarted" in {
      val (uploadConfig, uploadConfigJson) = genUploadConfig
      val v = ResponseUploadStarted(uploadConfig)
      val j = withHeader(ResponseUploadStarted.header)(
        "config" -> uploadConfigJson
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

  }
}

object JsonFormatsSpec {

  def genUploadConfig = {
    val (bitVector, bitVectorJson) = genBitVector

    (
      UploadConfig(bitVector),
      Json.obj(
        "serverData" -> bitVectorJson
      )
    )
  }

  def genFileLocation = {
    val fileId = Random.nextLong()
    val accessHash = Random.nextLong()

    (
      models.FileLocation(fileId, accessHash),
      Json.obj(
        "fileId" -> fileId.toString,
        "accessHash" -> accessHash.toString
      )
    )
  }

}
