package com.secretapp.backend.data.json.message.update

import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.data.json.message.struct._
import com.secretapp.backend.data.json.CommonJsonFormats._
import play.api.libs.json.Json

trait JsonFormats {

  import com.secretapp.backend.data.json.message._

  // UpdateMessage descendants
  implicit val seqUpdateFormat = Json.format[SeqUpdate]
  implicit val seqUpdateTooLongFormat = UnitFormat[SeqUpdateTooLong]
  implicit val weakUpdateFormat = Json.format[WeakUpdate]
  implicit val fatSeqUpdate = Json.format[FatSeqUpdate]

  // SeqUpdateMessage descendants
  implicit val avatarChangedFormat = Json.format[AvatarChanged]
  implicit val contactRegisteredFormat = Json.format[ContactRegistered]
  implicit val messageFormat = Json.format[Message]
  implicit val messageReadFormat = Json.format[MessageRead]
  implicit val messageReceivedFormat = Json.format[MessageReceived]
  implicit val messageSentFormat = Json.format[MessageSent]
  implicit val newDeviceFormat = Json.format[NewDevice]
  implicit val newFullDeviceFormat = Json.format[NewFullDevice]
  implicit val removeDeviceFormat = Json.format[RemoveDevice]

  // WeakUpdateMessage descendants
  implicit val userLastSeenFormat = Json.format[UserLastSeen]
  implicit val userOfflineFormat = Json.format[UserOffline]
  implicit val userOnlineFormat = Json.format[UserOnline]

}
