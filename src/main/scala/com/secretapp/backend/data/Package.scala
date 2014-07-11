package com.secretapp.backend.data

case class Package(authId : Long, sessionId : Long, message : ProtoMessageWrapper) {

  def replyWith(pm : ProtoMessage) : Package = {
    Package(authId, sessionId, ProtoMessageWrapper(message.messageId, pm))
  }

  def replyWith(f : (Long) => ProtoMessage) : Package = {
    replyWith(f(message.messageId))
  }

}

object Package {



}
