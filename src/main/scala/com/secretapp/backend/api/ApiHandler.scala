package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import scodec.bits._
import com.secretapp.backend.protocol._
import com.secretapp.backend.data._
import scalaz._
import Scalaz._
import java.util.concurrent.{ConcurrentSkipListSet, ConcurrentHashMap}

class ApiHandler(val authTable: ConcurrentHashMap[Long, ConcurrentSkipListSet[Long]]) extends Actor with ActorLogging with ApiService
{

  def receive = {
    case Received(data) =>
      val connection = sender()
      log.info(s"Received: $data ${data.length}")

      state = handleStream(state._1, state._2 ++ BitVector(data.toArray))
      log.info(s"state: $state")
      state._1 match {
        case DropParsing(e) =>
          log.info(s"DropParsing: $e")
          val dropMsg: ByteString = protoMessage.encode(Drop(5L, e)) match {
            case \/-(bs) => ByteString(bs.toByteArray)
            case -\/(e) => ByteString(e)
          }
          connection ! Write(dropMsg)
          connection ! Close
          context stop self

        case _ =>
          if (!sendBuffer.isEmpty) {
            log.info(s"Write($sendBuffer)")
            connection ! Write(sendBuffer)
            sendBuffer = ByteString()
          }
      }

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }

}
