package com.secretapp.backend.data.message.rpc.struct

import com.secretapp.backend.data.message.ProtobufMessage
import scodec.bits.BitVector

case class UploadConfig(serverData: BitVector) extends ProtobufMessage
