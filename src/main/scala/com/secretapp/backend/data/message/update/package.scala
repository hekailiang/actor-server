package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtobufMessage {
    val updateType: Int
  }

  trait CommonUpdateMessage extends ProtobufMessage {
    val commonUpdateType: Int
  }

  trait UpdateMessageObject {
    val updateType: Int
  }

  trait CommonUpdateMessageObject {
    val commonUpdateType: Int
  }
}
