package com.secretapp.backend.models

import scodec.bits._

@SerialVersionUID(1L)
case class AuthItem(
  id: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Int,
  authLocation: String, latitude: Option[Double], longitude: Option[Double],
  authId: Long, publicKeyHash: Long, deviceHash: BitVector
)

object AuthItem {
  def build(
    id: Int, appId: Int, deviceTitle: String, authTime: Int,
    authLocation: String, latitude: Option[Double], longitude: Option[Double],
    authId: Long, publicKeyHash: Long, deviceHash: BitVector
  ): AuthItem = {
    val appTitle = appId match {
      case 0 => "Android Official"
      case 1 => "Android Official"
      case 2 => "iOS Official"
      case 3 => "Web Official"
    }
    AuthItem(
      id = id, appId = appId, appTitle = appTitle, deviceTitle = deviceTitle, authTime = authTime,
      authLocation = authLocation, latitude = latitude, longitude = longitude,
      authId = authId, publicKeyHash = publicKeyHash, deviceHash = deviceHash
    )
  }
}
