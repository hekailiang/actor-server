package com.secretapp.backend.data.json.message.rpc.update

import java.util.UUID

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.update._
import play.api.libs.json._
import com.secretapp.backend.data.json.message._

trait JsonFormats {

  implicit val differenceUpdateFormat = Json.format[DifferenceUpdate]
  // FIXME: Looks like macro bug.
  val differenceFormat: Format[Difference] = Json.format[Difference]
  implicit val implicitDifferenceFormat = differenceFormat

  implicit val requestGetDifferenceFormat = Json.format[RequestGetDifference]
  implicit val requestGetStateFormat = UnitFormat[RequestGetState]

  implicit val responseSeqFormat = Json.format[ResponseSeq]
}
