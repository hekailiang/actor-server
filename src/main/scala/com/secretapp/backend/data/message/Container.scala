package com.secretapp.backend.data.message

import com.secretapp.backend.data.transport.MessageBox

case class Container(messages : Vector[MessageBox]) extends TransportMessage
object Container extends TransportMessageMessageObject {
  val header = 0xa
}
