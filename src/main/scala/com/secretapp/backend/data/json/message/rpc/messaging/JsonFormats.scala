package com.secretapp.backend.data.json.message.rpc.messaging

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.messaging._
import play.api.libs.json._

trait JsonFormats {
  implicit val encryptedAESKeyFormat = Json.format[EncryptedAESKey]
  implicit val encryptedAESMessageFormat = Json.format[EncryptedAESMessage]
  implicit val encryptedAESPackageFormat = Json.format[EncryptedAESPackage]
  implicit val encryptedUserAESKeysFormat = Json.format[EncryptedUserAESKeys]
  implicit val encryptedRSAMessageFormat = Json.format[EncryptedRSAMessage]
  implicit val encryptedRSAPackageFormat = Json.format[EncryptedRSAPackage]
  implicit val encryptedRSABroadcastFormat = Json.format[EncryptedRSABroadcast]

  implicit val requestSendMessageFormat = Json.format[RequestSendMessage]
  implicit val requestMessageReadFormat = Json.format[RequestMessageRead]
  implicit val requestMessageReceivedFormat = Json.format[RequestMessageReceived]
  implicit val requestSendGroupMessageFormat = Json.format[RequestSendGroupMessage]
}
