package com.secretapp.backend.data.message.rpc.struct

import com.secretapp.backend.data.message.ProtobufMessage

case class FileLocation(fileId : Long, accessHash : Long) extends ProtobufMessage
