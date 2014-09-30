package com.secretapp.backend.data.transport

import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import scodec.bits._
import scalaz._
import Scalaz._

trait TransportPackage {
  val authId: Long
  val sessionId: Long
  val messageBoxBytes: BitVector

  def replyWith(messageId: Long, tm: TransportMessage): TransportPackage

  def decodeMessageBox: String \/ MessageBox

  def encode: ByteString
}
