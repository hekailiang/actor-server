package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct
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
                avatar: Option[Avatar] = None,
                keyHashes: immutable.Set[Long] = Set()) {

  def accessHash(senderAuthId: Long): Long = User.getAccessHash(senderAuthId, this)

  def selfAccessHash = accessHash(authId)

  def toStruct(senderAuthId: Long) = {
    struct.User(
      uid = uid,
      accessHash = User.getAccessHash(senderAuthId, uid, accessSalt),
      keyHashes = keyHashes,
      name = name,
      sex = sex.toOption,
      phoneNumber = phoneNumber,
      avatar = avatar.map(_.toStruct))
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
