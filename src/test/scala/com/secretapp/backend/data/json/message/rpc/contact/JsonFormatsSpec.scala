package com.secretapp.backend.data.json.message.rpc.contact

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.contact._
import play.api.libs.json._
import JsonFormatsSpec._

import scala.collection.immutable
import scala.util.Random

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize ContactToImport" in {
      val (v, j) = genContactToImport
      testToAndFromJson[ContactToImport](j, v)
    }

    "(de)serialize PublicKeyRequest" in {
      val (v, j) = genPublicKeyRequest
      testToAndFromJson[PublicKeyRequest](j, v)
    }

  }

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestImportContacts" in {
      val (contactToImport, contactToImportJson) = genContactToImport
      val v = RequestImportContacts(immutable.Seq(contactToImport))
      val j = withHeader(RequestImportContacts.requestType)(
        "contacts" -> Json.arr(contactToImportJson)
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestPublicKeys" in {
      val (publicKeyRequest, publicKeyRequestJson) = genPublicKeyRequest
      val v = RequestPublicKeys(immutable.Seq(publicKeyRequest))
      val j = withHeader(RequestPublicKeys.requestType)(
        "keys" -> Json.arr(publicKeyRequestJson)
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

}

object JsonFormatsSpec {

  def genContactToImport = {
    val clientPhoneId = Random.nextLong()
    val phoneNumber = Random.nextLong()

    (
      ContactToImport(clientPhoneId, phoneNumber),
      Json.obj(
        "clientPhoneId" -> clientPhoneId.toString,
        "phoneNumber"   -> phoneNumber.toString
      )
    )
  }

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
}
