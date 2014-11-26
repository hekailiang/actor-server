package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.message.rpc.file._
import com.secretapp.backend.protocol.codecs.message.rpc.history._
import com.secretapp.backend.protocol.codecs.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import com.secretapp.backend.protocol.codecs.message.rpc.user._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object OkCodec extends Codec[Ok] {
  private val rpcResponseMessageCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint32)
    .\(ResponseGetDifference.header) { case d: ResponseGetDifference => d}(protoPayload(ResponseGetDifferenceCodec))
    .\(ResponseAuth.header) { case r: ResponseAuth => r}(protoPayload(ResponseAuthCodec))
    .\(ResponseSendAuthCode.header) { case r: ResponseSendAuthCode => r}(protoPayload(ResponseSendAuthCodeCodec))
    .\(ResponseEditAvatar.header) { case r: ResponseEditAvatar => r}(protoPayload(ResponseEditAvatarCodec))
    .\(ResponseEditGroupAvatar.header) { case r: ResponseEditGroupAvatar => r}(protoPayload(ResponseEditGroupAvatarCodec))
    .\(ResponseCreateGroup.header) { case r: ResponseCreateGroup => r}(protoPayload(ResponseCreateGroupCodec))
    .\(ResponseLoadDialogs.header) { case r: ResponseLoadDialogs => r}(protoPayload(ResponseLoadDialogsCodec))
    .\(ResponseGetFile.header) { case r: ResponseGetFile => r}(protoPayload(ResponseGetFileCodec))
    .\(ResponseGetAuthSessions.header) { case r: ResponseGetAuthSessions => r}(protoPayload(ResponseGetAuthSessionsCodec))
    .\(ResponseGetContacts.header) { case r: ResponseGetContacts => r}(protoPayload(ResponseGetContactsCodec))
    .\(ResponseLoadHistory.header) { case r: ResponseLoadHistory => r}(protoPayload(ResponseLoadHistoryCodec))
    .\(ResponseImportContacts.header) { case r: ResponseImportContacts => r}(protoPayload(ResponseImportContactsCodec))
    .\(ResponseSeqDate.header) { case r: ResponseSeqDate => r}(protoPayload(ResponseSeqDateCodec))
    .\(ResponseGetPublicKeys.header) { case r: ResponseGetPublicKeys => r}(protoPayload(ResponseGetPublicKeysCodec))
    .\(ResponseSearchContacts.header) { case r: ResponseSearchContacts => r}(protoPayload(ResponseSearchContactsCodec))
    .\(ResponseSeq.header) { case s: ResponseSeq => s}(protoPayload(ResponseSeqCodec))
    .\(ResponseCompleteUpload.header) { case r: ResponseCompleteUpload => r}(protoPayload(ResponseCompleteUploadCodec))
    .\(ResponseStartUpload.header) { case r: ResponseStartUpload => r}(protoPayload(ResponseStartUploadCodec))
    .\(ResponseVoid.header) { case r: ResponseVoid => r}(protoPayload(ResponseVoidCodec))
    .\(0, _ => true) { case a: Any => a }(new DiscriminatedErrorCodec("RpcOk"))

  private val codec = rpcResponseMessageCodec.pxmap[Ok](Ok.apply, Ok.unapply)

  def encode(r: Ok) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
