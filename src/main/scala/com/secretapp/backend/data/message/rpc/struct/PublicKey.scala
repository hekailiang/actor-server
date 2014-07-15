package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.data.message.ProtobufMessage
import scodec.bits.BitVector

case class PublicKey(uid: Int, keyHash: Long, key: BitVector) extends ProtobufMessage
