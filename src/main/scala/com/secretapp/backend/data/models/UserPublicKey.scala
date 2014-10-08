package com.secretapp.backend.data.models

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class UserPublicKey(uid: Int, publicKeyHash: Long, userAccessSalt: String, publicKey: BitVector, authId: Long)
