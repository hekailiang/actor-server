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
  with message.rpc.presence.JsonFormats
  with message.rpc.push.JsonFormats
  with message.rpc.update.JsonFormats
  with message.rpc.user.JsonFormats
  with message.struct.JsonFormats
  with message.update.JsonFormats
  with transport.JsonFormats
  with types.JsonFormats {

}
