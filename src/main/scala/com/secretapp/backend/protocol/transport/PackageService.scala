//package com.secretapp.backend.protocol.transport
//
//import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
//import com.secretapp.backend.session._
//import com.secretapp.backend.api.frontend._
//import com.secretapp.backend.data.message._
//import com.secretapp.backend.data.transport.{JsonPackage, MessageBox, MTPackage}
//import com.secretapp.backend.protocol.codecs.message.JsonMessageBoxCodec
//import scalaz._
//import Scalaz._
//
//trait PackageService extends PackageManagerService {
//  self: Connector =>
//  import SessionProtocol._
//  import context._
//
//  def handleMessage(p: MTPackage, mb: MessageBox): Unit = {
//    sessionRegion.tell(Envelope(p.authId, p.sessionId, HandleMessageBox(mb)), context.self)
//  }
//
//  def handlePackage(p: MTPackage): Unit = {
//    MessageBoxCodec.decodeValue(p.messageBoxBytes) match {
//      case \/-(mb) =>
//        handlePackageAuthentication(p, mb, MTConnection) { (p, pMsg) =>
//          pMsg match {
//            case Some(m) =>
//              log.info(s"m: $m")
//              val messageId = mb.messageId
//              context.self ! p.replyWith(messageId, m).right
//            case None =>
//          }
//
//          if (p.authId == 0L && p.sessionId == 0L) {
//            val reply = p.replyWith(mb.messageId, ResponseAuthId(currentAuthId)).right // TODO: move SessionActor logic from SA-45
//            context.self ! reply
//          } else {
//            log.debug(s"Sending to session $p")
//            sessionRegion.tell(Envelope(p.authId, p.sessionId, HandleMessageBox(mb)), context.self)
//          }
//        }
//      case -\/(e) =>
//        sendDrop(p, 0, new Exception(e))
//    }
//  }
//
//  //  TODO: DRY
//  def handleJsonPackage(p: JsonPackage): Unit = {
//    JsonMessageBoxCodec.decodeValue(p.messageBoxBytes) match {
//      case \/-(mb) =>
//        handlePackageAuthentication(p, mb, JsonConnection) { (p, pMsg) =>
//          pMsg match {
//            case Some(m) =>
//              log.info(s"m: $m")
//              val messageId = mb.messageId
//              context.self ! p.replyWith(messageId, m).right
//            case None =>
//          }
//
//          if (p.authId == 0L && p.sessionId == 0L) {
//            val reply = p.replyWith(mb.messageId, ResponseAuthId(currentAuthId)).right
//            context.self ! reply
//          } else {
//            log.debug(s"Sending to session $p")
//            sessionRegion.tell(Envelope(p.authId, p.sessionId, HandleMessageBox(mb)), context.self)
//          }
//        }
//      case -\/(e) =>
//        sendDrop(p, 0, new Exception(e))
//    }
//  }
//
//  def handleError(e: HandleError): Unit = e match {
//    case ParseError(msg) =>
//      val reply = MTPackage(0L, 0L, MessageBoxCodec.encodeValid(MessageBox(0L, Drop(0L, msg)))).left
//      context.self ! reply
//    case _ => log.error("unknown handle error")
//  }
//
//}
