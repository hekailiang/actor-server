//package com.secretapp.backend.api.frontend
//
//import akka.actor._
//import com.datastax.driver.core.{ Session => CSession }
//import com.secretapp.backend.api.SessionActor
//import com.secretapp.backend.data.transport._
//import com.secretapp.backend.persist.AuthIdRecord
//import com.secretapp.backend.protocol.codecs.transport.{TcpPackageCodec, PayloadCodec}
//import com.secretapp.backend.protocol.codecs.Implicits._
//import scala.collection.JavaConversions._
//import scala.util.Success
//import scodec.bits._
//import scalaz._
//import Scalaz._
//
//object SecurityFrontend {
//  def props(connection: ActorRef, sessionActor: ActorRef, authId: Long)(implicit csession: CSession) = {
//    Props(new SecurityFrontend(connection, sessionActor, authId)(csession))
//  }
//}
//
//class SecurityFrontend(connection: ActorRef, sessionActor: ActorRef, authId: Long)(implicit csession: CSession) extends Actor
//with ActorLogging
//{
//  import context.dispatcher
//  import context.system
//  import com.secretapp.backend.api.frontend.tcp.ClientFrontend._
//  import com.secretapp.backend.api.SessionActor._
//
//  val receiveBuffer = new java.util.LinkedList[Any]()
//  val authRecF = AuthIdRecord.getEntity(authId)
//  var sessionId: Long = _ // last sessionId from EncryptPackage's
//
//  override def preStart(): Unit = {
//    super.preStart()
//    authRecF.onComplete {
//      case Success(Some(authRec)) =>
//        context.become(receivePF)
//        receiveBuffer.foreach(self ! _)
//        receiveBuffer.clear()
//      case _ => silentClose()
//    }
//  }
//
//  def silentClose(): Unit = {
//    connection ! SilentClose
//    context stop self
//  }
//
//  // TODO
//  def decrypt(m: BitVector): Option[BitVector] = m.some
//
//  // TODO
//  def encrypt(m: BitVector): BitVector = m
//
//  def receivePF: Receive = {
//    case EncryptPackage(_, m) => // receive from TCP-frontend only
//      decrypt(m) match {
//        case Some(decrypted) =>
//          PayloadCodec.decodeValue(decrypted) match { // TODO: decode only transport message (without deep decoding)
//            case \/-(payload) =>
//              if (payload.sessionId != sessionId) {
//                if (sessionId != 0) sessionActor ! DisconnectSession(authId, sessionId)
//                sessionId = payload.sessionId
//              }
//              sessionActor ! Package(authId, payload.sessionId, payload.message)
//            case _ => silentClose()
//          }
//        case None => silentClose()
//      }
//    case payload: Payload => // receive from session actor only
//      if (payload.sessionId == sessionId) {
//        val m = encrypt(PayloadCodec.encodeValid(payload))
//        val p = TcpPackage(EncryptPackage(authId, m))
//        val blob = TcpPackageCodec.encodeValid(p)
//        connection ! ResponseToClient(blob)
//      }
//    case SilentClose => silentClose()
//  }
//
//  def receive = {
//    case m: Any => receiveBuffer.add(m)
//  }
//}
