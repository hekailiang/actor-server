package com.secretapp.backend.data.json.message.rpc.update

import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.update._
import play.api.libs.json.Json

trait JsonFormats {

  implicit val requestGetDifferenceFormat = Json.format[RequestGetDifference]
  implicit val requestGetStateFormat = UnitFormat[RequestGetState]

}
