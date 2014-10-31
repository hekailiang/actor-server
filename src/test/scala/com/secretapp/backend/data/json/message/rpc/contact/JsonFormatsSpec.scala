package com.secretapp.backend.data.json.message.rpc.contact

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.{RpcResponseMessage, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.contact._
import play.api.libs.json._
import JsonFormatsSpec._
import com.secretapp.backend.data.json.JsonSpec._
import scala.collection.immutable
import scala.util.Random
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {
    "(de)serialize PublicKeyRequest" in {
      val (v, j) = genPublicKeyRequest
      testToAndFromJson(j, v)
    }

    "(de)serialize PublicKeyResponse" in {
      val (v, j) = genPublicKeyResponse
      testToAndFromJson(j, v)
    }
  }

  "RpcRequestMessage (de)serializer" should {

//    "(de)serialize RequestImportContacts" in {
//      val (contactToImport, contactToImportJson) = genContactToImport
//      val v = RequestImportContacts(immutable.Seq(contactToImport))
//      val j = withHeader(RequestImportContacts.header)(
//        "contacts" -> Json.arr(contactToImportJson)
//      )
//      testToAndFromJson[RpcRequestMessage](j, v)
//    }

    "(de)serialize RequestPublicKeys" in {
      val (publicKeyRequest, publicKeyRequestJson) = genPublicKeyRequest
      val v = RequestPublicKeys(immutable.Seq(publicKeyRequest))
      val j = withHeader(RequestPublicKeys.header)(
        "keys" -> Json.arr(publicKeyRequestJson)
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

  "RpcResponseMessage (de)serializer" should {

//    "(de)serialize ResponseImportedContacts" in {
//      val (user, userJson) = genUser
//      val (importedContact, importedContactJson) = genImportedContact
//      val v = ResponseImportedContacts(immutable.Seq(user), immutable.Seq(importedContact))
//      val j = withHeader(ResponseImportedContacts.header)(
//        "users" -> Json.arr(userJson),
//        "contacts" -> Json.arr(importedContactJson)
//      )
//      testToAndFromJson[RpcResponseMessage](j, v)
//    }

    "(de)serialize ResponsePublicKeys" in {
      val (publicKeysResponse, publicKeysResponseJson) = genPublicKeyResponse
      val v = ResponsePublicKeys(immutable.Seq(publicKeysResponse))
      val j = withHeader(ResponsePublicKeys.header)(
        "keys" -> Json.arr(publicKeysResponseJson)
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

  }

}

object JsonFormatsSpec {
  def genPublicKeyRequest = {
    val uid = Random.nextInt()
    val accessHash = Random.nextLong()
    val keyHash = Random.nextLong()

    (
      PublicKeyRequest(uid, accessHash, keyHash),
      Json.obj(
        "uid"        -> uid,
        "accessHash" -> accessHash.toString,
        "keyHash"    -> keyHash.toString
      )
    )
  }

  def genPublicKeyResponse = {
    val uid = Random.nextInt()
    val keyHash = Random.nextLong()
    val (key, keyJson) = genBitVector

    (
      PublicKeyResponse(uid, keyHash, key),
      Json.obj(
        "uid" -> uid,
        "keyHash" -> keyHash.toString,
        "key" -> keyJson
      )
      )
  }

}
