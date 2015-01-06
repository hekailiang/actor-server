package com.secretapp.backend.models

@SerialVersionUID(1L)
case class UserEmail(
  id: Int,
  userId: Int,
  accessSalt: String,
  email: String,
  title: String
)
