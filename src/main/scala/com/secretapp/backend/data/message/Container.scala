package com.secretapp.backend.data.message

case class Container(messages : Vector[MessageBox]) extends TransportMessage
object Container {
  val header = 0xa
}
