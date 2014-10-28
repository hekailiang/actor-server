package com.secretapp.backend.models

import com.secretapp.backend.persist.KeyedEntity

@SerialVersionUID(1L)
case class AuthId(authId: Long, userId: Option[Int]) extends KeyedEntity[Long] {
  val key = authId
}
