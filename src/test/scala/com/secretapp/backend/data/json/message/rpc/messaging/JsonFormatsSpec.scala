package com.secretapp.backend.data.json.message.rpc.messaging

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.messaging._
import play.api.libs.json._
import JsonFormatsSpec._
import scala.collection.immutable
import scalaz._
import Scalaz._
import scodec.bits._

class JsonFormatsSpec extends JsonSpec {
  "(de)serializer" should {

    "(de)serialize EncryptedKey" in {
      val (v, j) = genEncryptedKey
      testToAndFromJson(j, v)
    }

//    TODO
//    "(de)serialize EncryptedMessage" in {
//      val (v, j) = genEncryptedMessage
//      testToAndFromJson(j, v)
//    }
  }

//  TODO
//  "RpcRequestMessage (de)serializer" should {
//    "(de)serialize RequestSendMessage" in {
//      val (encryptedMessage1, encryptedMessage1Json) = genEncryptedMessage
//      val v = RequestSendMessage(1, 2, 3, encryptedMessage1)
//      val j = withHeader(RequestSendMessage.requestType)(
//        "uid" -> 1,
//        "accessHash" -> "2",
//        "randomId" -> "3",
//        "message" -> encryptedMessage1Json
//      )
//      testToAndFromJson[RpcRequestMessage](j, v)
//    }
//
//    "(de)serialize RequestMessageRead" in {
//      val v = RequestMessageRead(1, 2, 3)
//      val j = withHeader(RequestMessageRead.requestType)(
//        "uid" -> 1,
//        "randomId" -> "2",
//        "accessHash" -> "3"
//      )
//      testToAndFromJson[RpcRequestMessage](j, v)
//    }
//
//    "(de)serialize RequestMessageReceived" in {
//      val v = RequestMessageReceived(1, 2, 3)
//      val j = withHeader(RequestMessageReceived.requestType)(
//        "uid" -> 1,
//        "randomId" -> "2",
//        "accessHash" -> "3"
//      )
//      testToAndFromJson[RpcRequestMessage](j, v)
//    }
//
//    "(de)serialize RequestSendGroupMessage" in {
//      val keyHash = hex"ac1d".bits
//      val message = hex"123456abcdf".bits
//      val v = RequestSendGroupMessage(1, 2, 3, keyHash, message)
//      val j = withHeader(RequestSendGroupMessage.requestType)(
//        "chatId" -> 1,
//        "accessHash" -> "2",
//        "randomId" -> "3",
//        "keyHash" -> keyHash.toBase64,
//        "message" -> message.toBase64
//      )
//      testToAndFromJson[RpcRequestMessage](j, v)
//    }
//  }
}

object JsonFormatsSpec {

  private val rand = new scala.util.Random()

  def genEncryptedKey = {
    val pkHash = rand.nextLong()
    val encKey = hex"ac1d".bits
    val key = EncryptedAESKey(pkHash, encKey)
//    val msg = EncryptedRSAMessage(encryptedMessage = BitVector(1, 2, 3),
//      keys = immutable.Seq(key),
//      ownKeys = immutable.Seq.empty)

    (
      key,
      Json.obj(
        "keyHash" -> pkHash.toString(),
        "aesEncryptedKey" -> encKey.toBase64
      )
    )
  }

//  def genEncryptedMessage = {
//    val (encryptedKey, encryptedKeyJson) = genEncryptedKey
//
//    (
//      EncryptedAESMessage(bitVector, immutable.Seq(encryptedKey)),
//      Json.obj(
//        "message" -> bitVectorJson,
//        "keys" -> Json.arr(encryptedKeyJson)
//      )
//    )
//  }
}
