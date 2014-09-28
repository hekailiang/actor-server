//package com.secretapp.backend.api.frontend
//
//import akka.actor._
//import akka.util.ByteString
//import com.secretapp.backend.data.message.{ RequestAuthId, ResponseAuthId, Drop }
//import com.secretapp.backend.data.models.AuthId
//import com.secretapp.backend.persist.AuthIdRecord
//import com.secretapp.backend.data.transport._
//import com.secretapp.backend.protocol.codecs._
//import com.secretapp.backend.protocol.codecs.Implicits._
//import com.secretapp.backend.protocol.codecs.transport.PayloadCodec
//import com.secretapp.backend.services.common.RandomService
//import com.datastax.driver.core.{ Session => CSession }
//import scalaz._
//import Scalaz._
//
//object KeyFrontend {
//  sealed trait KeyInitializationMessage
//
//  @SerialVersionUID(1L)
//  case class InitDH(p: EncryptPackage) extends KeyInitializationMessage
//
//  def props(connection: ActorRef)(implicit csession: CSession): Props = Props(new KeyFrontend(connection)(csession))
//}
//
//class KeyFrontend(connection: ActorRef)(implicit csession: CSession) extends Actor with ActorLogging with RandomService {
//  import KeyFrontend._
//  import com.secretapp.backend.api.frontend.tcp.ClientFrontend._
//
//  def silentClose(): Unit = {
//    connection ! SilentClose
//    context stop self
//  }
//
//  def receive = {
//    case InitDH(ep) =>
//      PayloadCodec.decodeValue(ep.message) match {
//        case \/-(p) =>
//          p.message.body match {
//            case _ if p.sessionId != 0 =>
//              dropClient(p.message.messageId, "sessionId must equal to 0", p.sessionId)
//            case RequestAuthId() =>
//              val newAuthId = rand.nextLong()
//              AuthIdRecord.insertEntity(AuthId(newAuthId, None))
//              val pkg = Package(0L, 0L, MessageBox(p.message.messageId, ResponseAuthId(newAuthId)))
//              connection ! ResponseToClient(protoPackage.encodeValid(pkg))
//            case _ =>
//              dropClient(p.message.messageId, "unknown message type in authorize mode")
//          }
//        case _ => silentClose()
//      }
//  }
//
//  def dropClient(messageId: Long, message: String, sessionId: Long = 0): Unit = {
//    val pkg = Package(0L, sessionId, MessageBox(messageId, Drop(messageId, message)))
//    connection ! ResponseToClientWithDrop(protoPackage.encodeValid(pkg))
//  }
//}
