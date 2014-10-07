package com.secretapp.backend.data.models

@SerialVersionUID(1l)
case class AuthSmsCode(phoneNumber: Long, smsHash: String, smsCode: String) // , appId: Int, authId: Long, lastSendAt: DateTime, counts: Int)
// add userId for single query - no userId or user is registered
