package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtobufMessage with MessageWithHeader

  trait SeqUpdateMessage extends ProtobufMessage with MessageWithHeader {
    def userIds: Set[Int]
  }

  trait WeakUpdateMessage extends ProtobufMessage with MessageWithHeader

  trait UpdateMessageObject extends MessageWithHeader

  trait SeqUpdateMessageObject extends MessageWithHeader

  trait WeakUpdateMessageObject extends MessageWithHeader
}
