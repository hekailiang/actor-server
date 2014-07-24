package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct

import scala.collection.immutable.Seq
import scala.util.Random
import com.secretapp.backend.data.types._
import com.secretapp.backend.protocol.codecs.ByteConstants
import com.secretapp.backend.Configuration
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
                phoneNumber: Long,
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

  def build(uid: Int, authId: Long, publicKey: BitVector, accessSalt: String, phoneNumber: Long, firstName: String,
            lastName: Option[String], sex: Sex = NoSex) = {
    val publicKeyHash = getPublicKeyHash(publicKey)
    User(uid = uid,
      authId = authId,
      publicKey = publicKey,
      publicKeyHash = publicKeyHash,
      accessSalt = accessSalt,
      phoneNumber = phoneNumber,
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

  def getPublicKeyHash(pk: BitVector): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }
}
