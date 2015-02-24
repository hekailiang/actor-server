package com.secretapp.backend.models

@SerialVersionUID(1L)
case class UserEmail(
  id: Int,
  email: String,
  title: String,
  accessSalt: String,
  userId: Int
)
