package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct

import scodec.bits._

@SerialVersionUID(1L)
case class AuthItem(
  id: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Int,
  authLocation: String, latitude: Option[Double], longitude: Option[Double],
  authId: Long, deviceHash: BitVector
) {
  def toStruct(currentAuthId: Long) = {
    val authHolder = if (currentAuthId == authId) {
      0
    } else {
      1
    }
    struct.AuthItem(
      id, authHolder, appId, appTitle, deviceTitle, authTime,
      authLocation, latitude, longitude
    )
  }
}

object AuthItem {
  def build(
    id: Int, appId: Int, deviceTitle: String, authTime: Int,
    authLocation: String, latitude: Option[Double], longitude: Option[Double],
    authId: Long, deviceHash: BitVector
  ): AuthItem = {
    val appTitle = appId match {
      case 0 => "Android Official"
      case 1 => "iOS Official"
      case 2 => "Web Official"
    }
    AuthItem(
      id = id, appId = appId, appTitle = appTitle, deviceTitle = deviceTitle, authTime = authTime,
      authLocation = authLocation, latitude = latitude, longitude = longitude,
      authId = authId, deviceHash = deviceHash
    )
  }
}
