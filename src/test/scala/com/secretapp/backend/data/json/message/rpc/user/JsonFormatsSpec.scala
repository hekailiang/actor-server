package com.secretapp.backend.data.json.message.rpc.user

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.user.RequestEditAvatar
import com.secretapp.backend.data.json.message.rpc.file.JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestEditAvatar" in {
      val (fileLocation, fileLocationJson) = genFileLocation
      val v = RequestEditAvatar(fileLocation)
      val j = withHeader(RequestEditAvatar.requestType)(
        "fileLocation" -> fileLocationJson
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

}
