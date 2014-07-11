package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import scodec.bits._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }


// TODO: replace connection : ActorRef hack with real sender (or forget it?)
class ApiHandler(connection : ActorRef)(implicit val session : CSession) extends Actor with ActorLogging with WrappedPackageService
{

  val handleActor = self

  private def handlePackage(p : Package, pMsg : Option[ProtoMessage]) : Unit = {
    pMsg match {
      case Some(m) => handleActor ! PackageToSend(p.replyWith(m).right)
      case None =>
    }

    if (p.authId == 0L && p.sessionId == 0L) {
      val reply = p.replyWith(ResponseAuthId(getAuthId)).right
      handleActor ! PackageToSend(reply)
    } else {
      p.message.body match {
        case Ping(randomId) =>
          val reply = p.replyWith(Pong(randomId)).right
          handleActor ! PackageToSend(reply)
//        case RpcRequest(rpcMessage) =>
//          rpcMessage match {
//            case SendSMSCode(phoneNumber, _, _) =>
//
//            case SignUp(phoneNumber, smsCodeHash, smsCode, _, _, _, _) =>
//            case SignIn(phoneNumber, smsCodeHash, smsCode) =>
//          }
//
//          s"rpc message#$rpcMessage is not implemented yet".left
//        case _ => s"unknown case for message".left
        case _ =>
      }
    }
  }

  private def handleError(e : HandleError) : Unit = e match {
    case ParseError(msg) =>
      val reply = Package(0L, 0L, ProtoMessageWrapper(0L, Drop(0L, msg))).left
      handleActor ! PackageToSend(reply)
    case _ => log.error("unknown handle error")
  }

  private def replyPackage(p : Package) : ByteString = {
    protoWrappedPackage.encode(p) match {
      case \/-(bs) => ByteString(bs.toByteArray)
      case -\/(e) => ByteString(e)
    }
  }

  def receive = {
    case PackageToSend(pe) => pe match {
      case \/-(p) =>
        connection ! Write(replyPackage(p))
      case -\/(p) =>
        connection ! Write(replyPackage(p))
        connection ! Close
        context stop self
    }

    case Received(data) =>
      val connection = sender()
      log.info(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }

}
