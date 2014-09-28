package com.secretapp.backend.api.frontend.ws

import akka.actor._
import akka.util.ByteString
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.transport.{MessageBox, JsonPackage}
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.data.message.Drop
import scodec.bits.BitVector
import spray.can.websocket
import spray.can.websocket.frame._
import spray.can.websocket.Send
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

// TODO: remove this hack
import spray.json._
import DefaultJsonProtocol._

import com.secretapp.backend.protocol.transport.{PackageService, WrappedPackageService, Connector}
import scalaz._
import Scalaz._

object WSConnector {
  def props(connection: ActorRef, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSConnector(connection, sessionRegion, session))
  }
}

class WSConnector(val serverConnection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends HttpServiceActor with Connector with websocket.WebSocketServerWorker with WrappedPackageService with PackageService {
  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5.seconds)

  def businessLogic: Receive = {
    //    case x @ (_: BinaryFrame | _: TextFrame) =>
    case x: TextFrame =>
      log.info(s"Frame: ${new String(x.payload.toArray)}")

      parseJsonPackage(x.payload) match {
        case \/-(p) => handleJsonPackage(p)
        case -\/(e) =>
          val mb = MessageBox(0L, Drop(0L, e))
          self ! JsonPackage(0L, 0L, mb).left
      }

    case pe: JsonPackageEither =>
      println(s"JsonPackageEither: $pe")
      pe match {
        case \/-(p) =>
          println(s"\\/-p: $p, ${new String(p.messageBoxBytes.toByteArray)}")
          send(TextFrame(p.toJson))
        case -\/(p) =>
          println(s"-\\/p: $p, ${new String(p.messageBoxBytes.toByteArray)}")
          send(TextFrame(p.toJson))
          serverConnection ! Send(CloseFrame())
      }

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
  }

  private def parseJsonPackage(data: ByteString): String \/ JsonPackage = {
    // TODO: remove this hack
    type JsonHead = Tuple3[Long, Long, JsValue]
    object MyJsonProtocol extends DefaultJsonProtocol {
      implicit object JsonHeadFormat extends RootJsonFormat[JsonHead] {
        def write(p: JsonHead) = ???

        def read(value: JsValue) = {
          value match {
            case JsArray(Seq(JsNumber(authId), JsNumber(sessionId), message: JsValue)) =>
              (authId.toLong, sessionId.toLong, message)
            case _ => throw new DeserializationException("JsonHead expected")
          }
        }
      }
    }
    import MyJsonProtocol._

    try {
      val jsonAst = data.utf8String.parseJson
      println(s"jsonAst: $jsonAst")
      val (authId, sessionId, message) = jsonAst.convertTo[JsonHead]
      JsonPackage(authId, sessionId, BitVector(message.toString.getBytes)).right
    } catch {
      case e: Throwable => e.getMessage.left
    }
  }
}

