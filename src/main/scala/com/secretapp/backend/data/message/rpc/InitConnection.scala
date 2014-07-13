package com.secretapp.backend.data.message.rpc

case class InitConnection(applicationId : Int,
                          applicationVersionIndex: Int,
                          deviceVendor : String,
                          deviceModel : String,
                          appLanguage : String,
                          osLanguage : String,
                          countryISO : String) extends RpcRequestMessage
