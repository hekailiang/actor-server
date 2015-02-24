package com.secretapp.backend.models

import org.joda.time.DateTime
import scodec.bits._

case class Group(
  id: Int,
  creatorUserId: Int,
  accessHash: Long,
  title: String,
  createdAt: DateTime
)
