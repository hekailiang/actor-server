package com.secretapp.backend.util

import com.secretapp.backend.Configuration
import com.secretapp.backend.models
import java.nio.ByteBuffer
import java.security.MessageDigest
import akka.actor.ActorSystem
import com.secretapp.backend.persist
import scala.concurrent.Future
import scala.util.Try
import com.secretapp.backend.models
import scala.concurrent.ExecutionContext.Implicits.global

object ACL {
  def secretKey()(implicit s: ActorSystem) =
    Try(s.settings.config.getString("secret-key")).getOrElse("topsecret")

  def hash(s: String): Long =
    ByteBuffer.wrap(MessageDigest.getInstance("MD5").digest(s.getBytes)).getLong

  def userAccessHash(authId: Long, uid: Int, accessSalt: String)(implicit s: ActorSystem): Long =
    hash(s"$authId:$uid:$accessSalt:${secretKey()}")

  def userAccessHash(authId: Long, u: models.User)(implicit s: ActorSystem): Long =
    userAccessHash(authId, u.uid, u.accessSalt)

  def fileAccessHash(fr: persist.File, fileId: Int)(implicit s: ActorSystem): Future[Long] =
    fr.getFileAccessSalt(fileId) map { accessSalt => hash(s"$fileId:$accessSalt:${secretKey()}") }
}
