package com.secretapp.backend.data.models

import scala.collection.immutable.Seq
import scala.util.Random
import com.secretapp.backend.data.types._
import scodec.bits.BitVector

case class User(publicKeyHash: Long,
                publicKey: BitVector,
                accessSalt: String,
                firstName: String,
                lastName: Option[String],
                sex: Sex,
                keyHashes: Seq[Long] = Seq()) {
  def accessHash: Long = 1L // TODO
}

object User {
  def build(publicKey: BitVector, firstName: String, lastName: Option[String], sex: Sex, publicKeyHash: Long = 1L): User = {
    val accessSalt = "salt" // new Random().nextString(30)
    User(publicKeyHash = publicKeyHash,
      publicKey = publicKey,
      accessSalt = accessSalt,
      firstName = firstName,
      lastName = lastName,
      sex = sex,
      keyHashes = Seq(1L))
  }
}
