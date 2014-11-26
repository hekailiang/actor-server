package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestInviteUser(groupOutPeer: struct.GroupOutPeer, user: struct.UserOutPeer) extends RpcRequestMessage {
  val header = RequestInviteUser.header
}

object RequestInviteUser extends RpcRequestMessageObject {
  val header = 0x45
}
