package com.secretapp.backend.data.message

package object update {

  trait UpdateMessage extends ProtobufMessage
  trait UpdateMessageObject {
    val updateType : Int
  }

}
