package com.secretapp.backend.protocol.transport

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.secretapp.backend.data.models.AuthId
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.session._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.{MessageBox, Package}
import scalaz._
import Scalaz._

trait PackageService extends PackageManagerService {
  self: Connector =>
  import SessionProtocol._
  import context._

  def handleMessage(p: Package, m: MessageBox): Unit = {
    sessionRegion.tell(HandlePackage(p), context.self)
  }

  def handlePackage(p: Package): Unit = {
    handlePackageAuthentication(p) { (p, pMsg) =>
      pMsg match {
        case Some(m) =>
          log.info(s"m: $m")
          //        val messageId = p.messageBox.messageId * System.currentTimeMillis() // TODO
          val messageId = p.messageBox.messageId
          context.self ! p.replyWith(messageId, m).right
        case None =>
      }

      if (p.authId == 0L && p.sessionId == 0L) {
        val reply = p.replyWith(p.messageBox.messageId, ResponseAuthId(currentAuthId)).right
        context.self ! reply
      }

    } andThen {
      case _ =>
        log.debug(s"Sending to session $p")
        sessionRegion.tell(HandlePackage(p), context.self)
    }
  }

  def handleError(e: HandleError): Unit = e match {
    case ParseError(msg) =>
      val reply = Package(0L, 0L, MessageBox(0L, Drop(0L, msg))).left
      context.self ! reply
    case _ => log.error("unknown handle error")
  }


}
