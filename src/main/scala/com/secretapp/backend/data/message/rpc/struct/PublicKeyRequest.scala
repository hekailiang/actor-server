package com.secretapp.backend.data.message.rpc.struct

import com.secretapp.backend.data.message.ProtobufMessage

case class PublicKeyRequest(uid: Int, accessHash: Long, keyHash: Long) extends ProtobufMessage
