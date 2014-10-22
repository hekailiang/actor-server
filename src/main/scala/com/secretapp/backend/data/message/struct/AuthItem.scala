package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class AuthItem(
  id: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Long,
  authLocation: String, latitude: Option[Double], longitude: Option[Double]
) extends ProtobufMessage {
  def toProto = protobuf.AuthItem(
    id, appId, appTitle, deviceTitle, authTime,
    authLocation, latitude, longitude
  )
}

object AuthItem {
  def build(
    id: Int, appId: Int, deviceTitle: String, authTime: Long,
    authLocation: String, latitude: Option[Double], longitude: Option[Double]
  ): AuthItem = {
    val appTitle = appId match {
      case 0 => "android-official"
      case 1 => "ios-official"
      case 2 => "web-official"
    }
    AuthItem(
      id = id, appId = appId, appTitle = appTitle, deviceTitle = deviceTitle, authTime = authTime,
      authLocation = authLocation, latitude = latitude, longitude = longitude
    )
  }

  def fromProto(authItem: protobuf.AuthItem): AuthItem = authItem match {
    case protobuf.AuthItem(
      id, appId, appTitle, deviceTitle, authTime,
      authLocation, latitude, longitude
    ) => AuthItem(
      id, appId, appTitle, deviceTitle, authTime,
      authLocation, latitude, longitude
    )
  }
}
