package com.secretapp.backend.data.models

import scala.collection.immutable.Seq
import com.secretapp.backend.data.types._
import scodec.bits.BitVector

case class User(publicKeyHash: Long,
                publicKey: BitVector,
                accessSalt: String,
                firstName: String,
                lastName: Option[String],
                sex: Sex) {
  def accessHash: Long = 1L // TODO

  def keyHashes = Seq(1L)
}

object User {
  def build(publicKey: BitVector, firstName: String, lastName: Option[String], sex: Sex): User = {
    ???
  }
}
