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
    .\(RequestGetDifference.header)       { case r: RequestGetDifference            => r } (protoPayload(RequestGetDifferenceCodec))
    .\(RequestGetState.header)            { case r: RequestGetState                 => r } (protoPayload(RequestGetStateCodec))
    .\(RequestAuthCode.header)            { case r: RequestAuthCode                 => r } (protoPayload(RequestAuthCodeCodec))
    .\(RequestSignIn.header)              { case r: RequestSignIn                   => r } (protoPayload(RequestSignInCodec))
    .\(RequestSignUp.header)              { case r: RequestSignUp                   => r } (protoPayload(RequestSignUpCodec))
    .\(RequestGetAuth.header)             { case r: RequestGetAuth                  => r } (protoPayload(RequestGetAuthCodec))
    .\(RequestLogout.header)              { case r: RequestLogout                   => r } (protoPayload(RequestLogoutCodec))
    .\(RequestRemoveAuth.header)          { case r: RequestRemoveAuth               => r } (protoPayload(RequestRemoveAuthCodec))
    .\(RequestRemoveAllOtherAuths.header) { case r: RequestRemoveAllOtherAuths      => r } (protoPayload(RequestRemoveAllOtherAuthsCodec))
    .\(RequestSendMessage.header)         { case r: RequestSendMessage              => r } (protoPayload(RequestSendMessageCodec))
    .\(RequestSendGroupMessage.header)    { case r: RequestSendGroupMessage         => r } (protoPayload(RequestSendGroupMessageCodec))
    .\(RequestImportContacts.header)      { case r: RequestImportContacts           => r } (protoPayload(RequestImportContactsCodec))
    .\(RequestPublicKeys.header)          { case r: RequestPublicKeys               => r } (protoPayload(RequestPublicKeysCodec))
    .\(RequestGetFile.header)             { case r: RequestGetFile                  => r } (protoPayload(RequestGetFileCodec))
    .\(RequestStartUpload.header)         { case r: RequestStartUpload              => r } (protoPayload(RequestUploadStartCodec))
    .\(RequestUploadPart.header)          { case r: RequestUploadPart               => r } (protoPayload(RequestUploadFileCodec))
    .\(RequestCompleteUpload.header)      { case r: RequestCompleteUpload           => r } (protoPayload(RequestCompleteUploadCodec))
    .\(SubscribeToOnline.header)          { case r: SubscribeToOnline               => r } (protoPayload(SubscribeToOnlineCodec))
    .\(UnsubscribeFromOnline.header)      { case r: UnsubscribeFromOnline           => r } (protoPayload(UnsubscribeFromOnlineCodec))
    .\(SubscribeToGroupOnline.header)     { case r: SubscribeToGroupOnline          => r } (protoPayload(SubscribeToGroupOnlineCodec))
    .\(UnsubscribeFromGroupOnline.header) { case r: UnsubscribeFromGroupOnline      => r } (protoPayload(UnsubscribeFromGroupOnlineCodec))
    .\(RequestSetOnline.header)           { case r: RequestSetOnline                => r } (protoPayload(RequestSetOnlineCodec))
    .\(RequestEditAvatar.header)          { case r: RequestEditAvatar               => r } (protoPayload(RequestEditAvatarCodec))
    .\(RequestEditName.header)            { case r: RequestEditName                 => r } (protoPayload(RequestEditNameCodec))
    .\(RequestRegisterGooglePush.header)  { case r: RequestRegisterGooglePush       => r } (protoPayload(RequestRegisterGooglePushCodec))
    .\(RequestRegisterApplePush.header)   { case r: RequestRegisterApplePush        => r } (protoPayload(RequestRegisterApplePushCodec))
    .\(RequestUnregisterPush.header)      { case r: RequestUnregisterPush           => r } (protoPayload(RequestUnregisterPushCodec))
    .\(RequestMessageReceived.header)     { case r: RequestMessageReceived          => r } (protoPayload(RequestMessageReceivedCodec))
    .\(RequestMessageRead.header)         { case r: RequestMessageRead              => r } (protoPayload(RequestMessageReadCodec))
    .\(RequestCreateGroup.header)          { case r: RequestCreateGroup               => r } (protoPayload(RequestCreateGroupCodec))
    .\(RequestEditGroupTitle.header)      { case r: RequestEditGroupTitle           => r } (protoPayload(RequestEditGroupTitleCodec))
    .\(RequestEditGroupAvatar.header)     { case r: RequestEditGroupAvatar          => r } (protoPayload(RequestEditGroupAvatarCodec))
    .\(RequestInviteUsers.header)         { case r: RequestInviteUsers              => r } (protoPayload(RequestInviteUsersCodec))
    .\(RequestLeaveGroup.header)           { case r: RequestLeaveGroup                => r } (protoPayload(RequestLeaveGroupCodec))
    .\(RequestRemoveUser.header)          { case r: RequestRemoveUser               => r } (protoPayload(RequestRemoveUserCodec))
    .\(RequestTyping.header)              { case r: RequestTyping                   => r } (protoPayload(RequestTypingCodec))
    .\(RequestGroupTyping.header)         { case r: RequestGroupTyping              => r } (protoPayload(RequestGroupTypingCodec))
    .\(0, _ => true) { case a: Any => a } (new DiscriminatedErrorCodec("Request"))

  private val codec = rpcRequestMessageCodec.pxmap[Request](Request.apply, Request.unapply)

  def encode(r: Request) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
