package com.secretapp.backend.data

import play.api.data.validation.ValidationError
import play.api.libs.json._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

package object json
  extends message.JsonFormats
  with message.rpc.JsonFormats
  with message.rpc.auth.JsonFormats
  with message.rpc.contact.JsonFormats
  with message.rpc.file.JsonFormats
  with message.rpc.messaging.JsonFormats
  with message.struct.JsonFormats
  with transport.JsonFormats
  with types.JsonFormats {

}
