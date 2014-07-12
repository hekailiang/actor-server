package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcMessage

case class RpcRequestBox(body : RpcMessage) extends TransportMessage
