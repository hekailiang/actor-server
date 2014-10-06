package com.secretapp.backend.data.json

package object message
  extends JsonFormats
  with rpc.JsonFormats
  with rpc.contact.JsonFormats
  with rpc.file.JsonFormats
  with rpc.messaging.JsonFormats
  with struct.JsonFormats {

}
