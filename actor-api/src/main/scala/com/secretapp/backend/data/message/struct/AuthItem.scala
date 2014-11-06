package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.models

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

  def fromModel(a: models.AuthItem, currentAuthId: Long) = {
    val authHolder = if (currentAuthId == a.authId) 0 else 1

    AuthItem(
      a.id, authHolder, a.appId, a.appTitle, a.deviceTitle, a.authTime,
      a.authLocation, a.latitude, a.longitude
    )
  }
}
