package com.secretapp.backend.data.message

import scalaz._
import Scalaz._

package object rpc {
  trait RpcRequest extends ProtoMessageWithHeader
  trait RpcResponse extends ProtoMessageWithHeader

  trait RpcMessage extends ProtobufMessage with ProtoMessageWithHeader
  trait RpcRequestMessage extends RpcMessage
  trait RpcResponseMessage extends RpcMessage

  trait RpcRequestObject extends ProtoMessageWithHeader
  trait RpcResponseObject extends ProtoMessageWithHeader
  trait RpcRequestMessageObject extends ProtoMessageWithHeader
  trait RpcResponseMessageObject extends ProtoMessageWithHeader
}
