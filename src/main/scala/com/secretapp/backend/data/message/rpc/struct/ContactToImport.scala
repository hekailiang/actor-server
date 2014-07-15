package com.secretapp.backend.data.message.rpc.struct

import com.secretapp.backend.data.message.ProtobufMessage

case class ContactToImport(clientPhoneId: Long, phoneNumber: Long) extends ProtobufMessage
