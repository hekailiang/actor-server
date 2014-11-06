package com.secretapp.backend.api

import akka.actor.ActorSystem
import akka.util.ByteString
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame._

class WSHeatingUpActor(host: String, port: Int)(implicit as: ActorSystem) extends websocket.WebSocketClientWorker {
  IO(UHttp) ! Http.Connect(host, port)

  val upgradeRequest = websocket.basicHandshakeRepuset("/")

  def businessLogic: Receive = {
    case websocket.UpgradedToWebSocket =>
      connection ! TextFrame(ByteString("$!#@%^$&*^*(#$&%".getBytes))
      context.become({
        case _: TextFrame =>
          connection ! CloseFrame()
        case _: Http.ConnectionClosed =>
          context stop self
      })
  }
}
