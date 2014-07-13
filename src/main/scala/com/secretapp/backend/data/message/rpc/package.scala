package com.secretapp.backend.data.message

import com.secretapp.backend.data.message._

package object rpc {

  trait RpcRequest
  trait RpcResponse

  trait RpcMessage extends ProtobufMessage
  trait RpcRequestMessage extends RpcMessage
  trait RpcResponseMessage extends RpcMessage
  trait RpcBidirectionalMessage extends RpcMessage


  trait RpcRequestMessageObject {
    val requestType : Int
  }
  trait RpcResponseMessageObject {
    val responseType : Int
  }
  trait RpcBidirectionalMessageObject {
    val bidirectionalType : Int
  }

}
