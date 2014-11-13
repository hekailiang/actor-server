package com.secretapp.backend.data.message

package object update {
  trait UpdateMessage extends ProtoMessageWithHeader

  trait SeqUpdateMessage extends ProtoMessageWithHeader {
    def userIds: Set[Int]
    def groupIds: Set[Int]
  }

  trait WeakUpdateMessage extends ProtoMessageWithHeader

  trait UpdateMessageObject extends ProtoMessageWithHeader

  trait SeqUpdateMessageObject extends ProtoMessageWithHeader

  trait WeakUpdateMessageObject extends ProtoMessageWithHeader
}
