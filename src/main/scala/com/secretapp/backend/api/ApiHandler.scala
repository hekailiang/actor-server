package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.io.Tcp._
import scodec.bits._
import java.util.concurrent.ConcurrentHashMap

class ApiHandler(val authTable: ConcurrentHashMap[Long, Long]) extends Actor with ActorLogging with ApiService  {

  def receive = {
    case Received(data) =>
      val connection = sender()
      log.info(s"Received: $data ${data.length}")

      state = handleReceivedBytes(state._1, state._2 ++ BitVector(data.toArray))(connection)
      log.info(s"state: $state")
      state._1 match {
        case DropParsing(e) =>
          log.info(s"DropParsing: $e")
          //          val dropMsg = Struct.dropCodec.encode(e.getMessage)
          //          connection ! Write(dropMsg)
          connection ! Close
          context stop self

        case _ =>
      }

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }

}
