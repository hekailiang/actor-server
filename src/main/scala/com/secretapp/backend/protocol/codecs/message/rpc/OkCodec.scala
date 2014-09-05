package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.update.{ CommonUpdateTooLong, CommonUpdate }
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.message.rpc.file._
import com.secretapp.backend.protocol.codecs.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.message.rpc.presence._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import com.secretapp.backend.protocol.codecs.message.rpc.user._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object OkCodec extends Codec[Ok] {
  private val rpcResponseMessageCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint32)
    .\(State.responseType) { case s: State => s }(protoPayload(StateCodec))
    .\(Difference.responseType) { case d: Difference => d }(protoPayload(DifferenceCodec))
    .\(ResponseAuth.responseType) { case r: ResponseAuth => r }(protoPayload(ResponseAuthCodec))
    .\(ResponseAuthCode.responseType) { case r: ResponseAuthCode => r }(protoPayload(ResponseAuthCodeCodec))
    .\(ResponseSendMessage.responseType) { case r: ResponseSendMessage => r }(protoPayload(ResponseSendMessageCodec))
    .\(ResponseImportedContacts.responseType) { case r: ResponseImportedContacts => r }(protoPayload(ResponseImportedContactsCodec))
    .\(ResponsePublicKeys.responseType) { case r: ResponsePublicKeys => r }(protoPayload(ResponsePublicKeysCodec))
    .\(ResponseFilePart.responseType) { case r: ResponseFilePart => r }(protoPayload(ResponseFilePartCodec))
    .\(ResponseUploadStarted.responseType) { case r: ResponseUploadStarted => r }(protoPayload(ResponseUploadStartCodec))
    .\(ResponsePartUploaded.responseType) { case r: ResponsePartUploaded => r }(protoPayload(ResponseFileUploadStartedCodec))
    .\(ResponseUploadCompleted.responseType) { case r: ResponseUploadCompleted => r }(protoPayload(FileUploadedCodec))
    .\(ResponseOnline.responseType) { case r: ResponseOnline => r }(protoPayload(ResponseOnlineCodec))
    .\(ResponseAvatarUploaded.responseType) { case r: ResponseAvatarUploaded => r }(protoPayload(ResponseAvatarUploadedCodec))
    .\(0, _ => true) { case a: Any => a }(new DiscriminatedErrorCodec("RpcOk"))

  private val codec = rpcResponseMessageCodec.pxmap[Ok](Ok.apply, Ok.unapply)

  def encode(r: Ok) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
