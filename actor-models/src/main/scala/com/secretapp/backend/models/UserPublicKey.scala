package com.secretapp.backend.models

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class UserPublicKey(
  userId: Int,
  publicKeyHash: Long,
  userAccessSalt: String,
  publicKey: BitVector,
  authId: Long
)
