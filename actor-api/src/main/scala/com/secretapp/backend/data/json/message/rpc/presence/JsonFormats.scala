package com.secretapp.backend.data.json.message.rpc.presence

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.json.message.struct._
import play.api.libs.json._

trait JsonFormats {

  val requestSetOnlineFormat = Json.format[RequestSetOnline]
  val subscribeForOnlineFormat = Json.format[SubscribeToOnline]
  val unsubscribeForOnlineFormat = Json.format[UnsubscribeFromOnline]

}
