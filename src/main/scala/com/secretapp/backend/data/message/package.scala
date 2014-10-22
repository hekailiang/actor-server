package com.secretapp.backend.data

package object message {
  trait MessageWithHeader {
    val header: Int
  }

  trait TransportMessage extends MessageWithHeader
  trait ProtobufMessage

  trait TransportMessageMessageObject extends MessageWithHeader
}
