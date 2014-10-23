package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class AuthItem(
  id: Int, authHolder: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Int,
  authLocation: String, latitude: Option[Double], longitude: Option[Double]
) extends ProtobufMessage {
  def toProto = protobuf.AuthItem(
    id, authHolder, appId, appTitle, deviceTitle, authTime,
    authLocation, latitude, longitude
  )
}

object AuthItem {
  def fromProto(authItem: protobuf.AuthItem): AuthItem = authItem match {
    case protobuf.AuthItem(
      id, authHolder, appId, appTitle, deviceTitle, authTime,
      authLocation, latitude, longitude
    ) => AuthItem(
      id, authHolder, appId, appTitle, deviceTitle, authTime,
      authLocation, latitude, longitude
    )
  }
}
