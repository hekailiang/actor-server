package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.message.rpc.file._
import com.secretapp.backend.protocol.codecs.message.rpc.history._
import com.secretapp.backend.protocol.codecs.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object OkCodec extends Codec[Ok] {
  private val rpcResponseMessageCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint32)
    .\(Difference.header) { case d: Difference => d}(protoPayload(DifferenceCodec))
    .\(ResponseAuth.header) { case r: ResponseAuth => r}(protoPayload(ResponseAuthCodec))
    .\(ResponseAuthCode.header) { case r: ResponseAuthCode => r}(protoPayload(ResponseAuthCodeCodec))
    .\(ResponseAvatarChanged.header) { case r: ResponseAvatarChanged => r}(protoPayload(ResponseAvatarChangedCodec))
    .\(ResponseCreateGroup.header) { case r: ResponseCreateGroup => r}(protoPayload(ResponseCreateGroupCodec))
    .\(ResponseLoadDialogs.header) { case r: ResponseLoadDialogs => r}(protoPayload(ResponseLoadDialogsCodec))
    .\(ResponseFilePart.header) { case r: ResponseFilePart => r}(protoPayload(ResponseFilePartCodec))
    .\(ResponseGetAuth.header) { case r: ResponseGetAuth => r}(protoPayload(ResponseGetAuthCodec))
    .\(ResponseGetContacts.header) { case r: ResponseGetContacts => r}(protoPayload(ResponseGetContactsCodec))
    .\(ResponseLoadHistory.header) { case r: ResponseLoadHistory => r}(protoPayload(ResponseLoadHistoryCodec))
    .\(ResponseImportedContacts.header) { case r: ResponseImportedContacts => r}(protoPayload(ResponseImportedContactsCodec))
    .\(ResponseMessageSent.header) { case r: ResponseMessageSent => r}(protoPayload(ResponseMessageSentCodec))
    .\(ResponsePublicKeys.header) { case r: ResponsePublicKeys => r}(protoPayload(ResponsePublicKeysCodec))
    .\(ResponseSearchContacts.header) { case r: ResponseSearchContacts => r}(protoPayload(ResponseSearchContactsCodec))
    .\(ResponseSeq.header) { case s: ResponseSeq => s}(protoPayload(ResponseSeqCodec))
    .\(ResponseUploadCompleted.header) { case r: ResponseUploadCompleted => r}(protoPayload(FileUploadedCodec))
    .\(ResponseUploadStarted.header) { case r: ResponseUploadStarted => r}(protoPayload(ResponseUploadStartedCodec))
    .\(ResponseVoid.header) { case r: ResponseVoid => r}(protoPayload(ResponseVoidCodec))
    .\(0, _ => true) { case a: Any => a }(new DiscriminatedErrorCodec("RpcOk"))

  private val codec = rpcResponseMessageCodec.pxmap[Ok](Ok.apply, Ok.unapply)

  def encode(r: Ok) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
