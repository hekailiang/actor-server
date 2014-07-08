package com.secretapp.backend.protocol



package object codecs {
  val varint = VarIntCodec.varint
  val protoLongs = LongsCodec.protoLongs
  val protoBytes = BytesCodec.protoBytes
  val sex = codecs.SexCodec.sex
  val protoString = StringCodec.protoString
  val packageCodec = PackageCodec.packageCodec
  val protoMessageWrapper = ProtoMessageWrapperCodec.protoMessageWrapper
  val protoMessage = ProtoMessageCodec.protoMessage
  val rpcResponseMessage = RpcResponseCodec.rpcResponseMessage
  val rpcResponse = RpcResponseCodec.rpcResponse
  val rpcRequestMessage = RpcRequestCodec.rpcRequestMessage
  val rpcRequest = RpcRequestCodec.rpcRequest
  val authorization = AuthorizationCodec.authorization
  val signIn = SignInCodec.signIn
  val signUp = SignUpCodec.signUp
  val sentSMSCode = SentSMSCodeCodec.sentSMSCode
  val sendSMSCode = SendSMSCodeCodec.sendSMSCode
  val drop = DropCodec.drop
  val newSession = NewSessionCodec.newSession
  val pong = PongCodec.pong
  val ping = PingCodec.ping
  val responseAuthId = ResponseAuthIdCodec.responseAuthId
  val requestAuthId = RequestAuthIdCodec.requestAuthId
}
