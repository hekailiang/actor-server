package com.secretapp.backend.data.message

import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox

case class Container(messages: immutable.Seq[MessageBox]) extends TransportMessage {
  val header = Container.header
}

object Container extends TransportMessageMessageObject {
  val header = 0xa
}
