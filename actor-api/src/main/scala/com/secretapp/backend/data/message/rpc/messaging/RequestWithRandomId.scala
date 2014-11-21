package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

trait RequestWithRandomId extends RpcRequestMessage {
  val randomId: Long
}
