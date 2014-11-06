package com.secretapp.backend.data.json.message.rpc.contact

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.contact._
import play.api.libs.json._
import com.secretapp.backend.data.json.message.struct._

trait JsonFormats {
  implicit val phoneToImportFormat  = Json.format[PhoneToImport]
  implicit val emailToImportFormat = Json.format[EmailToImport]

  implicit val publicKeyRequestFormat = Json.format[PublicKeyRequest]
  implicit val publicKeyResponseFormat = Json.format[PublicKeyResponse]

  implicit val requestImportContactsFormat = Json.format[RequestImportContacts]
  implicit val requestPublicKeysFormat = Json.format[RequestPublicKeys]

  implicit val responseImportedContactsFormat = Json.format[ResponseImportedContacts]
  implicit val responsePublicKeysFormat = Json.format[ResponsePublicKeys]
}
