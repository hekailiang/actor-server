package com.secretapp.backend.data.message

case class RequestRegisterGooglePush(projectId: Int, token: String) extends TransportMessage

object RequestRegisterGooglePush extends TransportMessageMessageObject {
  val header = 0x50
}
