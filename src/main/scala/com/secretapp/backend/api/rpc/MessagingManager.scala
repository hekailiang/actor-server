package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ CounterActor, CounterProtocol, SharedActors, UpdatesBroker }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.messaging.{ EncryptedMessage, RequestSendMessage, ResponseSendMessage }
import com.secretapp.backend.data.message.rpc.{ update => updateRpcProto }
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingManager(val handleActor: ActorRef, val currentUser: User)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
  import context.system

  implicit val timeout = Timeout(5.seconds)

  //val fupdatesBroker = UpdatesBroker.lookup(currentUser)
  def messageCounter: Future[ActorRef] = {
    SharedActors.lookup("message-counter") {
      system.actorOf(Props(new CounterActor("message")), "message-counter")
    }
  }

  def receive = {
    case RpcProtocol.Request(p, messageId,
      RequestSendMessage(
        uid, accessHash, randomId, useAesKey, aesMessage, messages
        )) =>
      handleRequestSendMessage(p, messageId)(uid, accessHash, randomId, useAesKey, aesMessage, messages)
  }
}

sealed trait MessagingService {
  self: MessagingManager =>

  import context.{ dispatcher, system }

  protected def handleRequestSendMessage(p: Package, messageId: Long)(uid: Int,
    accessHash: Long,
    randomId: Long,
    useAesKey: Boolean,
    aesMessage: Option[BitVector],
    messages: Seq[EncryptedMessage]) = {
    // TODO: check accessHash SA-21

    @inline /**
      * Generates updates list
      *
      * @param mid message id
      * @returns Tuple containing (uid, keyHash) of target updates sequence and update object
      */
    def mkUpdates(mid: Int): Seq[((Int, Long), updateProto.Message)] = {
      messages map { message =>
        // TODO: check conformity of keyHash to uid
        // TODO: check if mid is the same as uid or self uid
        val seqId = (message.uid, message.keyHash)

        (seqId, updateProto.Message(
          senderUID = currentUser.uid,
          destUID = uid,
          mid = mid,
          keyHash = message.keyHash,
          useAesKey = useAesKey,
          aesKey = message.aesEncryptedKey,
          message = useAesKey match {
            case true => aesMessage.get
            case false => message.message.get
          }))
      }
    }

    @inline
    def pushUpdates(updates: Seq[((Int, Long), updateProto.Message)]): Unit = {
      updates map {
        case ((uid, keyHash), update) =>
          for {
            updatesBroker <- UpdatesBroker.lookup(uid, keyHash)
          } yield {
            log.info("Update {}", update)
            updatesBroker ! UpdatesBroker.NewUpdate(update)
          }
      }
    }

    val fmid: Future[CounterProtocol.StateType] =
      messageCounter.flatMap(ask(_, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType])
    val fdestUserEntity = UserRecord.getEntity(uid)

    fdestUserEntity onComplete {
      case Success(Some(destUserEntity)) =>
        val updatesDestUserId = destUserEntity.uid
        val updatesDestPublicKeyHash = destUserEntity.publicKeyHash

        fmid onSuccess { case mid => pushUpdates(mkUpdates(mid)) }

        // FIXME: handle failures (retry or error, should not break seq)
        for {
          mid <- fmid
          updatesManager <- UpdatesBroker.lookup(currentUser.uid, currentUser.publicKeyHash)
          s <- ask(
            updatesManager, UpdatesBroker.NewUpdate(updateProto.MessageSent(mid, randomId))).mapTo[(Int, UUID)]
        } yield {
          val rsp = ResponseSendMessage(mid, s._1, uuid.encodeValid(s._2))
          handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
        }

      case Success(None) =>
        throw new RuntimeException("Destination user not found")
      case _ =>
      // TODO: handle error
    }
  }
}
