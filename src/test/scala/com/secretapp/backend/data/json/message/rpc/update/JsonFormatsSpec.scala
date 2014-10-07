package com.secretapp.backend.data.json.message.rpc.update

import java.util.UUID
import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.update._
import scalaz._
import Scalaz._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestGetDifference" in {
      val uuid = UUID.randomUUID()
      val v = RequestGetDifference(1, uuid.some)
      val j = withHeader(RequestGetDifference.requestType)(
        "seq" -> 1,
        "state" -> uuid.toString
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetState" in {
      val v = RequestGetState()
      val j = withHeader(RequestGetState.requestType)()
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

}
