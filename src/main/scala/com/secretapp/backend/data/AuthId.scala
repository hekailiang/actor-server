package com.secretapp.backend.data

import com.secretapp.backend.persist.KeyedEntity

case class AuthId(authId: Long, userId: Option[Long]) extends KeyedEntity[Long] {
  override val key = authId
  lazy val user: Option[User] = ???
}
