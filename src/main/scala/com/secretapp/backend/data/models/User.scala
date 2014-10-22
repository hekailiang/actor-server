package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.struct.{ AvatarImage, Avatar, FileLocation }
import scala.collection.immutable
import com.secretapp.backend.data.types._
import com.secretapp.backend.Configuration
import com.secretapp.backend.crypto.ec
import scodec.bits.BitVector
import java.security.MessageDigest
import java.nio.ByteBuffer
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class User(uid: Int,
                authId: Long,
                publicKeyHash: Long,
                publicKey: BitVector,
                phoneNumber: Long,
                accessSalt: String,
                name: String,
                sex: Sex,
                smallAvatarFileId: Option[Int] = None,
                smallAvatarFileHash: Option[Long] = None,
                smallAvatarFileSize: Option[Int] = None,
                largeAvatarFileId: Option[Int] = None,
                largeAvatarFileHash: Option[Long] = None,
                largeAvatarFileSize: Option[Int] = None,
                fullAvatarFileId: Option[Int] = None,
                fullAvatarFileHash: Option[Long] = None,
                fullAvatarFileSize: Option[Int] = None,
                fullAvatarWidth: Option[Int] = None,
                fullAvatarHeight: Option[Int] = None,
                keyHashes: immutable.Set[Long] = immutable.Set()) {

  def accessHash(senderAuthId: Long): Long = User.getAccessHash(senderAuthId, this)

  lazy val selfAccessHash = accessHash(authId)

  lazy val smallAvatarImage =
    for (
      id <- smallAvatarFileId;
      hash <- smallAvatarFileHash;
      size <- smallAvatarFileSize
    ) yield AvatarImage(FileLocation(id, hash), 100, 100, size)

  lazy val largeAvatarImage =
    for (
      id <- largeAvatarFileId;
      hash <- largeAvatarFileHash;
      size <- largeAvatarFileSize
    ) yield AvatarImage(FileLocation(id, hash), 200, 200, size)

  lazy val fullAvatarImage =
    for (
      id <- fullAvatarFileId;
      hash <- fullAvatarFileHash;
      size <- fullAvatarFileSize;
      w <- fullAvatarWidth;
      h <- fullAvatarHeight
    ) yield AvatarImage(FileLocation(id, hash), w, h, size)

  lazy val avatar =
    if (immutable.Seq(smallAvatarImage, largeAvatarImage, fullAvatarImage).exists(_.isDefined))
      Avatar(smallAvatarImage, largeAvatarImage, fullAvatarImage).some
    else
      None

  def toStruct(senderAuthId: Long) = {
    val hash = User.getAccessHash(senderAuthId, uid, accessSalt)
    struct.User(uid, hash, name, sex.toOption, keyHashes, phoneNumber, avatar)
  }
}

object User {
  import Configuration._

  def build(uid: Int, authId: Long, publicKey: BitVector, phoneNumber: Long, accessSalt: String, name: String, sex: Sex = NoSex) = {
    val publicKeyHash = ec.PublicKey.keyHash(publicKey)
    User(
      uid = uid,
      authId = authId,
      publicKey = publicKey,
      publicKeyHash = publicKeyHash,
      phoneNumber = phoneNumber,
      accessSalt = accessSalt,
      name = name,
      sex = sex,
      keyHashes = immutable.Set(publicKeyHash))
  }

  def getAccessHash(authId: Long, uid: Int, accessSalt: String): Long = {
    val str = s"$authId:$uid:$accessSalt:$secretKey"
    val digest = MessageDigest.getInstance("MD5")
    val res = digest.digest(str.getBytes)
    ByteBuffer.wrap(res).getLong
  }

  def getAccessHash(authId: Long, user: User): Long = getAccessHash(authId, user.uid, user.accessSalt)
}
