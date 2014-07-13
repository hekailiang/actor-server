package com.secretapp.backend.data.message.rpc.struct

import com.secretapp.backend.data.message.ProtobufMessage

case class ImportedContact(clientPhoneId : Long) extends ProtobufMessage
