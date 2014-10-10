package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.user._
import com.secretapp.backend.protocol.codecs.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.message.rpc.file._
import com.secretapp.backend.protocol.codecs.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.message.rpc.presence._
import com.secretapp.backend.protocol.codecs.message.rpc.push._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import com.secretapp.backend.protocol.codecs.message.rpc.typing._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestCodec extends Codec[Request] {
  val rpcRequestMessageCodec: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint32)
    .\(RequestGetDifference.requestType)      { case r: RequestGetDifference      => r } (protoPayload(RequestGetDifferenceCodec))
    .\(RequestGetState.requestType)           { case r: RequestGetState           => r } (protoPayload(RequestGetStateCodec))
    .\(RequestAuthCode.requestType)           { case r: RequestAuthCode           => r } (protoPayload(RequestAuthCodeCodec))
    .\(RequestSignIn.requestType)             { case r: RequestSignIn             => r } (protoPayload(RequestSignInCodec))
    .\(RequestSignUp.requestType)             { case r: RequestSignUp             => r } (protoPayload(RequestSignUpCodec))
    .\(RequestSendMessage.requestType)        { case r: RequestSendMessage        => r } (protoPayload(RequestSendMessageCodec))
    .\(RequestSendGroupMessage.requestType)   { case r: RequestSendGroupMessage   => r } (protoPayload(RequestSendGroupMessageCodec))
    .\(RequestImportContacts.requestType)     { case r: RequestImportContacts     => r } (protoPayload(RequestImportContactsCodec))
    .\(RequestPublicKeys.requestType)         { case r: RequestPublicKeys         => r } (protoPayload(RequestPublicKeysCodec))
    .\(RequestGetFile.requestType)            { case r: RequestGetFile            => r } (protoPayload(RequestGetFileCodec))
    .\(RequestStartUpload.requestType)        { case r: RequestStartUpload        => r } (protoPayload(RequestUploadStartCodec))
    .\(RequestUploadPart.requestType)         { case r: RequestUploadPart         => r } (protoPayload(RequestUploadFileCodec))
    .\(RequestCompleteUpload.requestType)     { case r: RequestCompleteUpload     => r } (protoPayload(RequestCompleteUploadCodec))
    .\(SubscribeToOnline.requestType)         { case r: SubscribeToOnline         => r } (protoPayload(SubscribeToOnlineCodec))
    .\(UnsubscribeFromOnline.requestType)     { case r: UnsubscribeFromOnline     => r } (protoPayload(UnsubscribeFromOnlineCodec))
    .\(RequestSetOnline.requestType)          { case r: RequestSetOnline          => r } (protoPayload(RequestSetOnlineCodec))
    .\(RequestEditAvatar.requestType)         { case r: RequestEditAvatar         => r } (protoPayload(RequestEditAvatarCodec))
    .\(RequestEditName.requestType)           { case r: RequestEditName           => r } (protoPayload(RequestEditNameCodec))
    .\(RequestRegisterGooglePush.requestType) { case r: RequestRegisterGooglePush => r } (protoPayload(RequestRegisterGooglePushCodec))
    .\(RequestUnregisterPush.requestType)     { case r: RequestUnregisterPush     => r } (protoPayload(RequestUnregisterPushCodec))
    .\(RequestMessageReceived.requestType)    { case r: RequestMessageReceived    => r } (protoPayload(RequestMessageReceivedCodec))
    .\(RequestMessageRead.requestType)        { case r: RequestMessageRead        => r } (protoPayload(RequestMessageReadCodec))
    .\(RequestCreateChat.requestType)         { case r: RequestCreateChat         => r } (protoPayload(RequestCreateChatCodec))
    .\(RequestInviteUsers.requestType)        { case r: RequestInviteUsers        => r } (protoPayload(RequestInviteUsersCodec))
    .\(RequestLeaveChat.requestType)          { case r: RequestLeaveChat          => r } (protoPayload(RequestLeaveChatCodec))
    .\(RequestRemoveUser.requestType)         { case r: RequestRemoveUser         => r } (protoPayload(RequestRemoveUserCodec))
    .\(RequestTyping.requestType)             { case r: RequestTyping             => r } (protoPayload(RequestTypingCodec))
    .\(RequestGroupTyping.requestType)        { case r: RequestGroupTyping        => r } (protoPayload(RequestGroupTypingCodec))
    .\(0, _ => true) { case a: Any => a } (new DiscriminatedErrorCodec("Request"))

  private val codec = rpcRequestMessageCodec.pxmap[Request](Request.apply, Request.unapply)

  def encode(r: Request) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
