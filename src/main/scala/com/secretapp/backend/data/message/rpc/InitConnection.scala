package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.data.message.ProtobufMessage

case class InitConnection(applicationId: Int,
                          applicationVersionIndex: Int,
                          deviceVendor: String,
                          deviceModel: String,
                          appLanguage: String,
                          osLanguage: String,
                          countryISO: Option[String]) extends ProtobufMessage
