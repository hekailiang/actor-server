package com.secretapp.backend.data.json.message.rpc.update

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.update._
import play.api.libs.json.Json
import com.secretapp.backend.data.json.message._

trait JsonFormats {

  //implicit val differenceUpdateFormat = Json.format[DifferenceUpdate]
  //implicit val differenceFormat = Json.format[Difference]

  implicit val requestGetDifferenceFormat = Json.format[RequestGetDifference]
  implicit val requestGetStateFormat = UnitFormat[RequestGetState]

}
