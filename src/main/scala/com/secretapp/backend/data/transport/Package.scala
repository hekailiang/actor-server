package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.{ MessageBox, TransportMessage }

case class Package(authId : Long, sessionId : Long, messageBox : MessageBox) {

  def replyWith(tm : TransportMessage) : Package = {
    Package(authId, sessionId, MessageBox(messageBox.messageId, tm))
  }

  def replyWith(f : (Long) => TransportMessage) : Package = {
    replyWith(f(messageBox.messageId))
  }

}
