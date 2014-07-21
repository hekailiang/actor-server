package com.secretapp.backend.data.models

import scala.collection.immutable.Seq
import scala.util.Random
import com.secretapp.backend.data.types._
import com.secretapp.backend.protocol.codecs.ByteConstants
import com.secretapp.backend.Configuration
import scodec.bits.BitVector
import java.security.MessageDigest
import java.nio.ByteBuffer

case class User(uid: Int,
                publicKeyHash: Long,
                publicKey: BitVector,
                accessSalt: String,
                firstName: String,
                lastName: Option[String],
                sex: Sex,
                keyHashes: Seq[Long] = Seq()) {
  def accessHash(senderPublicKey: BitVector): Long = User.getAccessHash(senderPublicKey, this)

  def selfAccessHash = accessHash(publicKey)
}

object User {
  import ByteConstants._
  import Configuration._

  def build(uid: Int, publicKey: BitVector, accessSalt: String, firstName: String, lastName: Option[String], sex: Sex = NoSex) = {
    val publicKeyHash = getPublicKeyHash(publicKey)
    User(uid = uid,
      publicKey = publicKey,
      publicKeyHash = publicKeyHash,
      accessSalt = accessSalt,
      firstName = firstName,
      lastName = lastName,
      sex = sex,
      keyHashes = Seq(publicKeyHash))
  }

  def getAccessHash(senderPublicKey: BitVector, uid: Int, accessSalt: String): Long = {
    val str = s"${senderPublicKey.toHex}:${uid}:${accessSalt}:${secretKey}"
    val digest = MessageDigest.getInstance("MD5")
    val res = digest.digest(str.getBytes)
    ByteBuffer.wrap(res).getLong
  }

  def getAccessHash(senderPublicKey: BitVector, user: User): Long = getAccessHash(senderPublicKey, user.uid, user.accessSalt)

  def getPublicKeyHash(pk: BitVector): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }
}
