package com.secretapp.backend.models

@SerialVersionUID(1L)
case class ApplePushCredentials(authId: Long, apnsKey: Int, token: String)
