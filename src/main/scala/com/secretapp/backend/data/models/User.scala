package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.struct.{AvatarImage, Avatar}
import scala.collection.immutable
import com.secretapp.backend.data.types._
import com.secretapp.backend.Configuration
import com.secretapp.backend.crypto.ec
import scodec.bits.BitVector
import java.security.MessageDigest
import java.nio.ByteBuffer
import scalaz._
import Scalaz._

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
                largeAvatarFileId: Option[Int] = None,
                largeAvatarFileHash: Option[Long] = None,
                fullAvatarFileId: Option[Int] = None,
                fullAvatarFileHash: Option[Long] = None,
                fullAvatarWidth: Option[Int] = None,
                fullAvatarHeight: Option[Int] = None,
                keyHashes: immutable.Set[Long] = Set()) {

  def accessHash(senderAuthId: Long): Long = User.getAccessHash(senderAuthId, this)

  lazy val selfAccessHash = accessHash(authId)

  lazy val smallAvatarImage =
    for (
      id <- smallAvatarFileId;
      hash <- smallAvatarFileHash
    ) yield AvatarImage(FileLocation(id, hash), 100, 100)

  lazy val largeAvatarImage =
    for (
      id <- largeAvatarFileId;
      hash <- largeAvatarFileHash
    ) yield AvatarImage(FileLocation(id, hash), 200, 200)

  lazy val fullAvatarImage =
    for (
      id <- largeAvatarFileId;
      hash <- largeAvatarFileHash;
      w <- fullAvatarWidth;
      h <- fullAvatarHeight
    ) yield AvatarImage(FileLocation(id, hash), w, h)

  lazy val avatar =
    if (Seq(smallAvatarImage, largeAvatarImage, fullAvatarImage).exists(_.isDefined))
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
      keyHashes = Set(publicKeyHash))
  }

  def getAccessHash(authId: Long, uid: Int, accessSalt: String): Long = {
    val str = s"$authId:$uid:$accessSalt:$secretKey"
    val digest = MessageDigest.getInstance("MD5")
    val res = digest.digest(str.getBytes)
    ByteBuffer.wrap(res).getLong
  }

  def getAccessHash(authId: Long, user: User): Long = getAccessHash(authId, user.uid, user.accessSalt)
}
