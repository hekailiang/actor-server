package com.secretapp.backend.data.json.message.rpc.contact

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.contact._
import play.api.libs.json._

trait JsonFormats {
  implicit val contactToImportFormat  = Json.format[ContactToImport]
  implicit val publicKeyRequestFormat = Json.format[PublicKeyRequest]
  implicit val requestImportContactsFormat = Json.format[RequestImportContacts]
  implicit val requestPublicKeysFormat = Json.format[RequestPublicKeys]
}
