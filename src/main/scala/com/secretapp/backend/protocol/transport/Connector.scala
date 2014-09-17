package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.Timeout

trait Connector extends Actor with ActorLogging {
  val sessionRegion: ActorRef
  implicit val timeout: Timeout
}
