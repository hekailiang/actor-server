package com.secretapp.backend.data

package object message {

  trait TransportMessage
  trait ProtobufMessage

  trait TransportMessageMessageObject {
    val header: Int
  }

}
