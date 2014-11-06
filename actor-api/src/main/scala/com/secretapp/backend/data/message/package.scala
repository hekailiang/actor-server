package com.secretapp.backend.data

package object message {
  trait ProtoMessageWithHeader {
    val header: Int
  }

  trait TransportMessage extends ProtoMessageWithHeader
  trait ProtobufMessage

  trait TransportMessageMessageObject extends ProtoMessageWithHeader
}
