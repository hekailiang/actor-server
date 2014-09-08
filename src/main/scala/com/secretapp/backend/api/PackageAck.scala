package com.secretapp.backend.api

import akka.io.Tcp._

case class PackageAck(stamp: Int) extends Event
