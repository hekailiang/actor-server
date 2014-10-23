package com.secretapp.backend.data.models

import scodec.bits._

case class Group(
  id: Int, creatorUserId: Int, accessHash: Long, title: String,
  keyHash: BitVector, publicKey: BitVector
)
