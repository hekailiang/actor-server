package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtobufMessage with ProtoMessageWithHeader

  trait SeqUpdateMessage extends ProtobufMessage with ProtoMessageWithHeader {
    def userIds: Set[Int]
  }

  trait WeakUpdateMessage extends ProtobufMessage with ProtoMessageWithHeader

  trait UpdateMessageObject extends ProtoMessageWithHeader

  trait SeqUpdateMessageObject extends ProtoMessageWithHeader

  trait WeakUpdateMessageObject extends ProtoMessageWithHeader
}
