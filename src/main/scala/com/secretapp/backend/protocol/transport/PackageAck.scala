package com.secretapp.backend.protocol.transport

import akka.io.Tcp._

case class PackageAck(stamp: Int) extends Event
