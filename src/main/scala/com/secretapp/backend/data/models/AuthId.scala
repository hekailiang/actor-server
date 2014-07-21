package com.secretapp.backend.data.models

import com.secretapp.backend.persist.KeyedEntity

case class AuthId(authId: Long, userId: Option[Int]) extends KeyedEntity[Long] {
  override val key = authId
  lazy val user: Option[User] = None // ???
}
