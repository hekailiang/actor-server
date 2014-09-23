package com.secretapp.backend.protocol

import scodec.Codec

package object codecs {

  //  common types
  val varint = common.VarIntCodec
  val protoLongs = common.LongsCodec
  val protoBytes = common.BytesCodec
  val protoBool = common.BooleanCodec
  def protoPayload[A](payloadCodec: Codec[A]) = new common.PayloadBytesCodec[A](payloadCodec)
  val protoSex = common.SexCodec
  val protoString = common.StringCodec

  // messages
  val protoTransportMessage = message.TransportMessageCodec

  // rpc
//  val protoRpcRequest = message.rpc.RpcRequestCodec
//  val protoRpcResponse = message.rpc.RpcResponseCodec

  // transport
  val protoPackage = transport.MTPackageCodec
  val protoPackageBox = transport.MTPackageBoxCodec

}
