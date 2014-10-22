package com.secretapp.backend.data.message

import scalaz._
import Scalaz._

package object rpc {
  trait RpcRequest extends MessageWithHeader
  trait RpcResponse extends MessageWithHeader

  trait RpcMessage extends ProtobufMessage with MessageWithHeader
  trait RpcRequestMessage extends RpcMessage
  trait RpcResponseMessage extends RpcMessage

  trait RpcRequestObject extends MessageWithHeader
  trait RpcResponseObject extends MessageWithHeader
  trait RpcRequestMessageObject extends MessageWithHeader
  trait RpcResponseMessageObject extends MessageWithHeader
}
