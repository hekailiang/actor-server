package com.secretapp.backend.data.message

import scalaz._
import Scalaz._

package object rpc {
  trait RpcRequest {
    def rpcType: Int
  }
  trait RpcResponse {
    def rpcType: Int
  }

  trait RpcMessage extends ProtobufMessage {
    def header: Int
  }
  trait RpcRequestMessage extends RpcMessage
  trait RpcResponseMessage extends RpcMessage

  trait RpcRequestObject {
    val rpcType: Int
  }
  trait RpcResponseObject {
    val rpcType: Int
  }
  trait RpcRequestMessageObject {
    val requestType: Int // TODO: Rename to `header`
  }
  trait RpcResponseMessageObject {
    val responseType: Int
  }
}
