package com.secretapp.backend.data

case class AuthId(authId: Long, userId: Option[Long]) {
  lazy val user: Option[User] = ???
}
