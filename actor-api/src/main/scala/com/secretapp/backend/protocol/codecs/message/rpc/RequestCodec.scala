package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.history._
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
    .\(RequestAddContact.header) { case r: RequestAddContact => r}(protoPayload(RequestAddContactCodec))
    .\(RequestSendAuthCode.header) { case r: RequestSendAuthCode => r}(protoPayload(RequestSendAuthCodeCodec))
    .\(RequestSendAuthCall.header) { case r: RequestSendAuthCall => r}(protoPayload(RequestSendAuthCallCodec))
    .\(RequestClearChat.header) { case r: RequestClearChat => r}(protoPayload(RequestClearChatCodec))
    .\(RequestCompleteUpload.header) { case r: RequestCompleteUpload => r}(protoPayload(RequestCompleteUploadCodec))
    .\(RequestCreateGroup.header) { case r: RequestCreateGroup => r}(protoPayload(RequestCreateGroupCodec))
    .\(RequestDeleteChat.header) { case r: RequestDeleteChat => r}(protoPayload(RequestDeleteChatCodec))
    .\(RequestDeleteGroup.header) { case r: RequestDeleteGroup => r}(protoPayload(RequestDeleteGroupCodec))
    .\(RequestEditAvatar.header) { case r: RequestEditAvatar => r}(protoPayload(RequestEditAvatarCodec))
    .\(RequestEditGroupAvatar.header) { case r: RequestEditGroupAvatar => r}(protoPayload(RequestEditGroupAvatarCodec))
    .\(RequestEditGroupTitle.header) { case r: RequestEditGroupTitle => r}(protoPayload(RequestEditGroupTitleCodec))
    .\(RequestEditName.header) { case r: RequestEditName => r}(protoPayload(RequestEditNameCodec))
    .\(RequestEditUserLocalName.header) { case r: RequestEditUserLocalName => r}(protoPayload(RequestEditUserLocalNameCodec))
    .\(RequestEncryptedRead.header) { case r: RequestEncryptedRead => r}(protoPayload(RequestEncryptedReadCodec))
    .\(RequestEncryptedReceived.header) { case r: RequestEncryptedReceived => r}(protoPayload(RequestEncryptedReceivedCodec))
    .\(RequestGetAuthSessions.header) { case r: RequestGetAuthSessions => r}(protoPayload(RequestGetAuthSessionsCodec))
    .\(RequestGetContacts.header) { case r: RequestGetContacts => r}(protoPayload(RequestGetContactsCodec))
    .\(RequestGetDifference.header) { case r: RequestGetDifference => r}(protoPayload(RequestGetDifferenceCodec))
    .\(RequestGetFile.header) { case r: RequestGetFile => r}(protoPayload(RequestGetFileCodec))
    .\(RequestGetState.header) { case r: RequestGetState => r}(protoPayload(RequestGetStateCodec))
    .\(RequestImportContacts.header) { case r: RequestImportContacts => r}(protoPayload(RequestImportContactsCodec))
    .\(RequestInviteUser.header) { case r: RequestInviteUser => r}(protoPayload(RequestInviteUserCodec))
    .\(RequestLeaveGroup.header) { case r: RequestLeaveGroup => r}(protoPayload(RequestLeaveGroupCodec))
    .\(RequestLoadDialogs.header) { case r: RequestLoadDialogs => r}(protoPayload(RequestLoadDialogsCodec))
    .\(RequestLoadHistory.header) { case r: RequestLoadHistory => r}(protoPayload(RequestLoadHistoryCodec))
    .\(RequestSignOut.header) { case r: RequestSignOut => r}(protoPayload(RequestSignOutCodec))
    .\(RequestDeleteMessage.header) { case r: RequestDeleteMessage => r}(protoPayload(RequestDeleteMessageCodec))
    .\(RequestMessageRead.header) { case r: RequestMessageRead => r}(protoPayload(RequestMessageReadCodec))
    .\(RequestMessageReceived.header) { case r: RequestMessageReceived => r}(protoPayload(RequestMessageReceivedCodec))
    .\(RequestGetPublicKeys.header) { case r: RequestGetPublicKeys => r}(protoPayload(RequestGetPublicKeysCodec))
    .\(RequestRegisterApplePush.header) { case r: RequestRegisterApplePush => r}(protoPayload(RequestRegisterApplePushCodec))
    .\(RequestRegisterGooglePush.header) { case r: RequestRegisterGooglePush => r}(protoPayload(RequestRegisterGooglePushCodec))
    .\(RequestTerminateAllSessions.header) { case r: RequestTerminateAllSessions => r}(protoPayload(RequestTerminateAllSessionsCodec))
    .\(RequestTerminateSession.header) { case r: RequestTerminateSession => r}(protoPayload(RequestTerminateSessionCodec))
    .\(RequestRemoveAvatar.header) { case r: RequestRemoveAvatar => r}(protoPayload(RequestRemoveAvatarCodec))
    .\(RequestRemoveContact.header) { case r: RequestRemoveContact => r}(protoPayload(RequestRemoveContactCodec))
    .\(RequestRemoveGroupAvatar.header) { case r: RequestRemoveGroupAvatar => r}(protoPayload(RequestRemoveGroupAvatarCodec))
    .\(RequestRemoveUser.header) { case r: RequestRemoveUser => r}(protoPayload(RequestRemoveUserCodec))
    .\(RequestSearchContacts.header) { case r: RequestSearchContacts => r}(protoPayload(RequestSearchContactsCodec))
    .\(RequestSendEncryptedMessage.header) { case r: RequestSendEncryptedMessage => r}(protoPayload(RequestSendEncryptedMessageCodec))
    .\(RequestSendMessage.header) { case r: RequestSendMessage => r}(protoPayload(RequestSendMessageCodec))
    .\(RequestSetOnline.header) { case r: RequestSetOnline => r}(protoPayload(RequestSetOnlineCodec))
    .\(RequestSignIn.header) { case r: RequestSignIn => r}(protoPayload(RequestSignInCodec))
    .\(RequestSignUp.header) { case r: RequestSignUp => r}(protoPayload(RequestSignUpCodec))
    .\(RequestStartUpload.header) { case r: RequestStartUpload => r}(protoPayload(RequestUploadStartCodec))
    .\(RequestTyping.header) { case r: RequestTyping => r}(protoPayload(RequestTypingCodec))
    .\(RequestUnregisterPush.header) { case r: RequestUnregisterPush => r}(protoPayload(RequestUnregisterPushCodec))
    .\(RequestUploadPart.header) { case r: RequestUploadPart => r}(protoPayload(RequestUploadFileCodec))
    .\(RequestSubscribeToGroupOnline.header) { case r: RequestSubscribeToGroupOnline => r}(protoPayload(RequestSubscribeToGroupOnlineCodec))
    .\(RequestSubscribeToOnline.header) { case r: RequestSubscribeToOnline => r}(protoPayload(RequestSubscribeToOnlineCodec))
    .\(RequestSubscribeFromGroupOnline.header) { case r: RequestSubscribeFromGroupOnline => r}(protoPayload(RequestSubscribeFromGroupOnlineCodec))
    .\(RequestSubscribeFromOnline.header) { case r: RequestSubscribeFromOnline => r}(protoPayload(RequestSubscribeFromOnlineCodec))
    .\(0, _ => true) { case a: Any => a } (new DiscriminatedErrorCodec("Request"))

  private val codec = rpcRequestMessageCodec.pxmap[Request](Request.apply, Request.unapply)

  def encode(r: Request) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
