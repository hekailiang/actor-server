package com.secretapp.backend.util

import com.secretapp.backend.Configuration
import akka.actor.ActorSystem
import scala.util.Try
import com.secretapp.backend.models
import java.nio.ByteBuffer
import java.security.MessageDigest

object ACL {
  def userAccessHash(authId: Long, uid: Int, accessSalt: String): Long =
    ByteBuffer.wrap(
      MessageDigest.getInstance("MD5").digest(
        s"$authId:$uid:$accessSalt:${Configuration.secretKey}".getBytes
      )
    ).getLong

  def userAccessHash(authId: Long, u: models.User): Long =
    userAccessHash(authId, u.uid, u.accessSalt)
}
