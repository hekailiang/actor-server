package com.secretapp.backend.data.models

import scodec.bits.BitVector

case class UserPublicKey(uid: Int, publicKeyHash: Long, userAccessSalt: String, publicKey: BitVector, authId: Long)
