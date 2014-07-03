package com.secretapp.backend.data

case class PackageHead(authId: Long,
  sessionId: Long,
  messageId: Long,
  messageLength: Int)
{
  val messageBitLength = messageLength * 8L
}
case class PackageMessage(message: ProtoMessage)
case class Package(head: PackageHead, message: ProtoMessage)
