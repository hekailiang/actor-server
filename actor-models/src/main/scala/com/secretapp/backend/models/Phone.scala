package com.secretapp.backend.models

@SerialVersionUID(1L)
case class Phone(number: Long, userId: Int, userAccessSalt: String, userName: String, userSex: Sex = NoSex) {
  def toUserPhone(id: Int, userId: Int, accessSalt: String): UserPhone = {
    UserPhone(
      id = id,
      userId = userId,
      accessSalt = accessSalt,
      number = number,
      title = "Mobile phone"
    )
  }
}
