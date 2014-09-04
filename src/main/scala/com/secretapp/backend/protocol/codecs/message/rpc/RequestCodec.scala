package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.message.rpc.file._
import com.secretapp.backend.protocol.codecs.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.message.rpc.presence._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestCodec extends Codec[Request] {
  val rpcRequestMessageCodec: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint32)
    .\(RequestGetDifference.requestType) { case r: RequestGetDifference => r } (protoPayload(RequestGetDifferenceCodec))
    .\(RequestGetState.requestType) { case r: RequestGetState => r } (protoPayload(RequestGetStateCodec))
    .\(RequestAuthCode.requestType) { case r: RequestAuthCode => r } (protoPayload(RequestAuthCodeCodec))
    .\(RequestSignIn.requestType) { case r: RequestSignIn => r } (protoPayload(RequestSignInCodec))
    .\(RequestSignUp.requestType) { case r: RequestSignUp => r } (protoPayload(RequestSignUpCodec))
    .\(RequestSendMessage.requestType) { case r: RequestSendMessage => r } (protoPayload(RequestSendMessageCodec))
    .\(RequestImportContacts.requestType) { case r: RequestImportContacts => r } (protoPayload(RequestImportContactsCodec))
    .\(RequestPublicKeys.requestType) { case r: RequestPublicKeys => r } (protoPayload(RequestPublicKeysCodec))
    .\(RequestGetFile.requestType) { case r: RequestGetFile => r } (protoPayload(RequestGetFileCodec))
    .\(RequestStartUpload.requestType) { case r: RequestStartUpload => r } (protoPayload(RequestUploadStartCodec))
    .\(RequestUploadPart.requestType) { case r: RequestUploadPart => r } (protoPayload(RequestUploadFileCodec))
    .\(RequestCompleteUpload.requestType) { case r: RequestCompleteUpload => r } (protoPayload(RequestCompleteUploadCodec))
    .\(SubscribeForOnline.requestType) { case r: SubscribeForOnline => r } (protoPayload(SubscribeForOnlineCodec))
    .\(UnsubscribeForOnline.requestType) { case r: UnsubscribeForOnline => r } (protoPayload(UnsubscribeForOnlineCodec))
    .\(RequestSetOnline.requestType) { case r: RequestSetOnline => r } (protoPayload(RequestSetOnlineCodec))
    .\(0, _ => true) { case a: Any => a } (new DiscriminatedErrorCodec("Request"))

  private val codec = rpcRequestMessageCodec.pxmap[Request](Request.apply, Request.unapply)

  def encode(r: Request) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
