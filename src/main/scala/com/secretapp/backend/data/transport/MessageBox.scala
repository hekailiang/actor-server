package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.TransportMessage

@SerialVersionUID(1L)
case class MessageBox(messageId: Long, body: TransportMessage)
