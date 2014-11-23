package com.secretapp.backend.models

case class AuthSmsCodeCreationRequest(smsHash: String, smsCode: String) {

  def toAuthSmsCode(phone: Long): AuthSmsCode = AuthSmsCode(phone, smsHash, smsCode)

}
