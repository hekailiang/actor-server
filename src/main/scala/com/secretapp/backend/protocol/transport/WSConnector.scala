package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }

object WSConnector {
  def props(connection: ActorRef, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSConnector(connection, sessionRegion, session))
  }
}

class WSConnector(val connection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends Connector with ActorLogging with WrappedPackageService with PackageService {
  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5.seconds)

  def receive = ???

}
