package com.secretapp.backend.data

package object message {

  trait TransportMessage {
    def header: Int
  }
  trait ProtobufMessage

  trait TransportMessageMessageObject {
    def header: Int
  }

}
