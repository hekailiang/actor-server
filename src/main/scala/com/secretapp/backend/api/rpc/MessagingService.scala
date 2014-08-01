package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.SharedActors
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.api.counters.{ CounterActor, CounterProtocol }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.messaging.{ EncryptedMessage, RequestSendMessage, ResponseSendMessage }
import com.secretapp.backend.data.message.rpc.{ update => updateRpcProto }
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.UserPublicKeyRecord
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.common.PackageCommon._
import akka.persistence._
import java.util.UUID
import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingServiceActor(val handleActor: ActorRef, val updatesBrokerRegion: ActorRef, val currentUser: User, sessionId: Long)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
  import context.system

  implicit val timeout = Timeout(5.seconds)

  val counterId = s"{currentUser.authId}"

  def receive: Actor.Receive = {
    case RpcProtocol.Request(p, messageId,
      RequestSendMessage(
        uid, accessHash, randomId, useAesKey, aesMessage, messages
        )) =>
      handleRequestSendMessage(p, messageId)(uid, accessHash, randomId, useAesKey, aesMessage, messages)
  }
}

sealed trait MessagingService {
  self: MessagingServiceActor =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  // Stores (userId, publicKeyHash) -> authId associations
  val authIds = new TrieMap[(Int, Long), Future[Option[Long]]]

  protected def handleRequestSendMessage(p: Package, messageId: Long)(uid: Int,
    accessHash: Long,
    randomId: Long,
    useAesKey: Boolean,
    aesMessage: Option[BitVector],
    messages: Seq[EncryptedMessage]) = {
    // TODO: check accessHash SA-21

    def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long]] = {
      authIds.get((uid, publicKeyHash)) match {
        case Some(f) => f
        case None =>
          val f = UserPublicKeyRecord.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
          authIds.put((uid, publicKeyHash), f)
          f
      }
    }

    @inline
    def pushUpdates(): Unit = {
      messages map { message =>
        authIdFor(message.uid, message.publicKeyHash) onComplete {
          case Success(Some(authId)) =>
            log.info(s"Pushing message ${message}")
            updatesBrokerRegion ! NewUpdateEvent(authId, NewMessage(currentUser.uid, message, aesMessage))
          case x => log.error(s"Cannot find authId for uid=${message.uid} publicKeyHash=${message.publicKeyHash} ${x}")
        }
      }
    }

    val fdestUserEntity = UserRecord.getEntity(uid)

    fdestUserEntity onComplete {
      case Success(Some(destUserEntity)) =>
        val updatesDestUserId = destUserEntity.uid
        val updatesDestPublicKeyHash = destUserEntity.publicKeyHash

        pushUpdates()

        // FIXME: handle failures (retry or error, should not break seq)
        for {
          s <- ask(
            updatesBrokerRegion, NewUpdateEvent(currentUser.authId, NewMessageSent(randomId))).mapTo[UpdatesBroker.State]
        } yield {
          val rsp = ResponseSendMessage(s._2, s._1, uuid.encodeValid(s._3))
          handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
        }

      case Success(None) =>
        throw new RuntimeException("Destination user not found")
      case _ =>
      // TODO: handle error
    }
  }
}
