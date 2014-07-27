package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct
import scala.collection.immutable.Seq
import scala.util.Random
import com.secretapp.backend.data.types._
import com.secretapp.backend.protocol.codecs.ByteConstants
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
                accessSalt: String,
                firstName: String,
                lastName: Option[String],
                sex: Sex,
                keyHashes: Seq[Long] = Seq()) {
  def accessHash(senderAuthId: Long): Long = User.getAccessHash(senderAuthId, this)

  def selfAccessHash = accessHash(authId)

  def toStruct(senderAuthId: Long) = {
    struct.User(uid = this.uid, accessHash = User.getAccessHash(senderAuthId, this.uid, this.accessSalt),
      keyHashes = this.keyHashes, firstName = this.firstName, lastName = this.lastName, sex = this.sex.toOption)
  }
}

object User {
  import ByteConstants._
  import Configuration._

  def build(uid: Int, authId: Long, publicKey: BitVector, accessSalt: String, firstName: String,
            lastName: Option[String], sex: Sex = NoSex) = {
    val publicKeyHash = ec.PublicKey.keyHash(publicKey)
    User(uid = uid,
      authId = authId,
      publicKey = publicKey,
      publicKeyHash = publicKeyHash,
      accessSalt = accessSalt,
      firstName = firstName,
      lastName = lastName,
      sex = sex,
      keyHashes = Seq(publicKeyHash))
  }

  def getAccessHash(authId: Long, uid: Int, accessSalt: String): Long = {
    val str = s"$authId:$uid:$accessSalt:$secretKey"
    val digest = MessageDigest.getInstance("MD5")
    val res = digest.digest(str.getBytes)
    ByteBuffer.wrap(res).getLong
  }

  def getAccessHash(authId: Long, user: User): Long = getAccessHash(authId, user.uid, user.accessSalt)
}
