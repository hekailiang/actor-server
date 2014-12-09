package com.secretapp.backend.models

import scala.collection.immutable
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class User(
  uid: Int,
  authId: Long,
  publicKeyHash: Long,
  publicKey: BitVector,
  phoneNumber: Long,
  accessSalt: String,
  name: String,
  countryCode: String,
  sex: Sex,
  keyHashes: immutable.Set[Long] = immutable.Set()
)
