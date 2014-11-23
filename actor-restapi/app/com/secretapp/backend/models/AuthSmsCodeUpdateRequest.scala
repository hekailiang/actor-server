package com.secretapp.backend.models

import utils.OptSet._

case class AuthSmsCodeUpdateRequest(smsHash: Option[String], smsCode: Option[String]) {

  def update(c: AuthSmsCode): AuthSmsCode =
    c
      .optSet(smsHash)((c, v) => c.copy(smsHash = v))
      .optSet(smsCode)((c, v) => c.copy(smsCode = v))

}
