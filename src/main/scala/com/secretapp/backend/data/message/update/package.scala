package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtobufMessage {
    val updateHeader: Int
  }

  trait SeqUpdateMessage extends ProtobufMessage {
    val seqUpdateHeader: Int

    def userIds: Set[Int]
  }

  trait WeakUpdateMessage extends ProtobufMessage {
    val weakUpdateHeader: Int
  }

  trait UpdateMessageObject {
    val updateHeader: Int
  }

  trait SeqUpdateMessageObject {
    val seqUpdateHeader: Int
  }

  trait WeakUpdateMessageObject {
    val weakUpdateHeader: Int
  }
}
