package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.Timeout
import com.secretapp.backend.data.transport.MTPackage

case class StopConnector(p: MTPackage)

trait Connector extends Actor with ActorLogging {
  val sessionRegion: ActorRef
  implicit val timeout: Timeout
}
