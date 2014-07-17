package com.secretapp.backend.data.message

import com.secretapp.backend.data.message._

package object rpc {
  trait RpcRequest
  trait RpcResponse

  trait RpcMessage extends ProtobufMessage
  trait RpcRequestMessage extends RpcMessage
  trait RpcResponseMessage extends RpcMessage

  trait RpcRequestObject {
    val rpcType: Int
  }
  trait RpcResponseObject {
    val rpcType: Int
  }
  trait RpcRequestMessageObject {
    val requestType: Int
  }
  trait RpcResponseMessageObject {
    val responseType: Int
  }
}
