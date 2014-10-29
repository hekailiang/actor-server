package com.secretapp.backend.util

import java.nio.ByteBuffer
import java.security.MessageDigest
import akka.actor.ActorSystem
import scala.util.Try
import com.secretapp.backend.models

object ACL {
  def secretKey()(implicit s: ActorSystem) =
    Try(s.settings.config.getString("secret-key")).getOrElse("topsecret")

  def userAccessHash(authId: Long, uid: Int, accessSalt: String)(implicit s: ActorSystem): Long =
    ByteBuffer.wrap(
      MessageDigest.getInstance("MD5").digest(
        s"$authId:$uid:$accessSalt:${secretKey()}".getBytes
      )
    ).getLong

  def userAccessHash(authId: Long, u: models.User)(implicit s: ActorSystem): Long =
    userAccessHash(authId, u.uid, u.accessSalt)
}
