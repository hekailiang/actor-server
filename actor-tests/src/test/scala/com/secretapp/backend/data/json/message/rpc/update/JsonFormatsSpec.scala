package com.secretapp.backend.data.json.message.rpc.update

import java.util.UUID
import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.{RpcResponseMessage, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update.contact.ContactRegistered
import play.api.libs.json.Json
import scala.collection.immutable
import scala.util.Random
import scalaz._
import Scalaz._
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  import JsonFormatsSpec._

  "(de)serializer" should {

    "(de)serialize DifferenceUpdate" in {
      val (v, j) = genDifferenceUpdate
      testToAndFromJson(j, v)
    }

    "(de)serialize Difference" in {
      val state = UUID.randomUUID()
      val (user, userJson) = genUser
      val (differenceUpdate, differenceUpdateJson) = genDifferenceUpdate
      val v = Difference(1, state.some, immutable.Seq(user), immutable.Seq(differenceUpdate), true)
      val j = Json.obj(
        "seq" -> 1,
        "state" -> state.toString,
        "users" -> Json.arr(userJson),
        "updates" -> Json.arr(differenceUpdateJson),
        "needMore" -> true
      )
      testToAndFromJson(j, v)
    }

  }

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestGetDifference" in {
      val state = UUID.randomUUID()
      val v = RequestGetDifference(1, state.some)
      val j = withHeader(RequestGetDifference.header)(
        "seq" -> 1,
        "state" -> state.toString
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetState" in {
      val v = RequestGetState()
      val j = withHeader(RequestGetState.header)()
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

  "RpcResponseMessage (de)serializer" should {

    "(de)serialize ResponseSeq" in {
      val state = UUID.randomUUID()
      val v = ResponseSeq(1, state.some)
      val j = withHeader(ResponseSeq.header)(
        "seq" -> 1,
        "state" -> state.toString
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

  }

}

object JsonFormatsSpec extends JsonSpec {

  def genDifferenceUpdate = {
    val userId = Random.nextInt()
    (
      DifferenceUpdate(ContactRegistered(userId)),
      Json.obj(
        "body" -> withHeader(ContactRegistered.header)(
          "uid" -> userId
        )
      )
    )
  }
}
