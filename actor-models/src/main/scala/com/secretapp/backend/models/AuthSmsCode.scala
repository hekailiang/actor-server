package com.secretapp.backend.models

@SerialVersionUID(1L)
case class AuthSmsCode(phoneNumber: Long, smsHash: String, smsCode: String)
