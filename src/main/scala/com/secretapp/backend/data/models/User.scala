package com.secretapp.backend.data.models

import com.secretapp.backend.data.types._
import scodec.bits.BitVector

case class User(publicKeyHash: Long,
                publicKey: BitVector,
                accessSalt: String,
                firstName: String,
                lastName: Option[String],
                sex: Sex)

object User {
  def build(publicKey: BitVector, firstName: String, lastName: Option[String], sex: Sex): User = {
    ???
  }
}
