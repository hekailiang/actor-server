package com.secretapp.backend.session

trait MessageIdGenerator {
  var sessionMessageId: Long = 0L

  sealed trait MessageIdType { val remainder: Int }
  case object ResponseMsgId extends MessageIdType { val remainder = 1 } // RpcResponse
  case object TransportMsgId extends MessageIdType { val remainder = 2 } // MessageAck, Ping
  case object UpdateMsgId extends MessageIdType { val remainder = 3 } // Updates

  def getMessageId[T <: MessageIdType](msgIdType: T): Long = {
    sessionMessageId += (sessionMessageId % 4) + (4 - msgIdType.remainder)
    sessionMessageId
  }
}
