package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.data.message.ProtobufMessage

case class PublicKey(uid : Int, keyHash : Long, key : List[Byte]) extends ProtobufMessage
