package com.secretapp.backend.data.json.message.rpc.user

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.{RpcResponseMessage, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarChanged, RequestEditAvatar}
import com.secretapp.backend.data.json.message.rpc.file.JsonFormatsSpec._
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._

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

  "RpcResponseMessage (de)serializer" should {

    "(de)serialize ResponseAvatarChanged" in {
      val (avatar, avatarJson) = genAvatar
      val v = ResponseAvatarChanged(avatar)
      val j = withHeader(ResponseAvatarChanged.responseType)(
        "avatar" -> avatarJson
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

  }

}
