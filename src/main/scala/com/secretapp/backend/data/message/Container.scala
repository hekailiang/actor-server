package com.secretapp.backend.data.message

import scala.collection.immutable.Seq
import com.secretapp.backend.data.transport.MessageBox

case class Container(messages : Seq[MessageBox]) extends TransportMessage
object Container extends TransportMessageMessageObject {
  val header = 0xa
}
