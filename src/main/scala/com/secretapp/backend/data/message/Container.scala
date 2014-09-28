package com.secretapp.backend.data.message

import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox

case class Container(messages: immutable.Seq[MessageBox]) extends TransportMessage {
  override val header = Container.header
}
object Container extends TransportMessageMessageObject {
  override val header = 0xa
}
