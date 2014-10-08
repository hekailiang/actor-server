package com.secretapp.backend.data.models

import scodec.bits._

case class GroupChat(
  id: Int, creatorUserId: Int, accessHash: Long, title: String,
  keyHash: BitVector, publicKey: BitVector
)
