package com.secretapp.backend.data.message

import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox

@SerialVersionUID(1L)
case class Container(messages: immutable.Seq[MessageBox]) extends TransportMessage {
  val header = Container.header
}

object Container extends TransportMessageMessageObject {
  val header = 0x0A
}
