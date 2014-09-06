package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtobufMessage {
    val updateType: Int
  }

  trait CommonUpdateMessage extends ProtobufMessage {
    val commonUpdateType: Int

    def userIds: Set[Int]
  }

  trait WeakUpdateMessage extends ProtobufMessage {
    val weakUpdateType: Int
  }

  trait UpdateMessageObject {
    val updateType: Int
  }

  trait CommonUpdateMessageObject {
    val commonUpdateType: Int
  }

  trait WeakUpdateMessageObject {
    val weakUpdateType: Int
  }
}
