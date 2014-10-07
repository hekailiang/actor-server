package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.TransportMessage

@SerialVersionUID(1l)
case class MessageBox(messageId: Long, body: TransportMessage)
