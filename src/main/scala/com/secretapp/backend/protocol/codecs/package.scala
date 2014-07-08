package com.secretapp.backend.protocol

package object codecs {

  //  common types
  val varint = common.VarIntCodec
  val protoLongs = common.LongsCodec
  val protoBytes = common.BytesCodec
  val protoSex = common.SexCodec
  val protoString = common.StringCodec

  // messages
  val protoMessage = ProtoMessageCodec

  // rpc
  val protoRpcRequest = message.rpc.RpcRequestCodec
  val protoRpcResponse = message.rpc.RpcResponseCodec

  // transport
  val protoPackage = transport.PackageCodec
  val protoWrappedPackage = transport.WrappedPackageCodec

}
