package com.secretapp.backend.data.models

import com.github.nscala_time.time.Imports._

case class AuthSmsCode(phoneNumber: Long, smsHash: String, smsCode: String) // , appId: Int, authId: Long, lastSendAt: DateTime, counts: Int)
