package com.secretapp.backend.data.message

package object update {
  trait CommonUpdateMessage extends ProtobufMessage {
    val commonUpdateType: Int
  }

  trait UpdateMessage extends ProtobufMessage {
    val updateType: Int
  }

  trait UpdateMessageObject {
    val updateType: Int
  }
}
