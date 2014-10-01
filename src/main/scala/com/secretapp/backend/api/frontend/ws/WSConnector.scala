package com.secretapp.backend.api.frontend.ws

import akka.actor._
import akka.util.ByteString
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.transport.{MessageBox, JsonPackage}
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.data.message.Drop
import com.secretapp.backend.api.frontend._
import scodec.bits.BitVector
import spray.can.websocket
import spray.can.websocket.frame._
import spray.can.websocket.Send
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor
import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.protocol.transport.Connector
import com.secretapp.backend.util.parser.BS._
import scalaz._
import Scalaz._

object WSConnector {
  def props(connection: ActorRef, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSConnector(connection, sessionRegion, session))
  }
}

class WSConnector(val serverConnection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends HttpServiceActor with Connector with websocket.WebSocketServerWorker {
  val transport = JsonConnection

  def businessLogic: Receive = {
    case frame: TextFrame =>
      log.info(s"Frame: ${new String(frame.payload.toArray)}")
      parseJsonPackage(frame.payload) match {
        case \/-(p) => handlePackage(p)
        case -\/(e) => sendDrop(e)
      }
    case x: FrameCommandFailed =>
      log.error(s"frame command failed: $x")
    case ResponseToClient(bs) =>
      log.info(s"ResponseToClient: $bs")
      send(TextFrame(bs))
    case ResponseToClientWithDrop(bs) =>
      send(TextFrame(bs))
      silentClose()
    case SilentClose =>
      silentClose()
  }

  private def parseJsonPackage(data: ByteString): String \/ JsonPackage = {
    val jsonParser = for {
      _ <- skipTill(c => c == '[' || isWhiteSpace(c) || c == '"')
      authId <- parseSigned
      _ <- skipTill (c => c == ',' || isWhiteSpace(c) || c == '"')
      sessionId <- parseSigned
      _ <- skipTill (c => c == ',' || isWhiteSpace(c) || c == '"')
      _ <- skipRightTill(c => c == ']' || isWhiteSpace(c))
      message <- slice
    } yield JsonPackage(authId, sessionId, BitVector(message.toByteBuffer))
    Try(runParser(jsonParser, data)) match {
      case Success(jp) => jp.right
      case Failure(e) =>
        // TODO: silentClose ???
        s"""Expected JSON format: ["authId", "sessionId", {/* message box */}]\nDebug: ${e.getMessage}""".left
    }
  }

  def silentClose(): Unit = {
    send(CloseFrame())
    context.stop(self)
  }
}
