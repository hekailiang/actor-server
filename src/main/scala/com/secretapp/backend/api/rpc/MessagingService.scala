package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.rpc.{ Error, Ok }
import com.secretapp.backend.data.message.rpc.messaging.{ EncryptedMessage, RequestSendMessage, ResponseSendMessage }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.{ UserPublicKeyRecord, UserRecord }
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingServiceActor(
  val handleActor: ActorRef, val updatesBrokerRegion: ActorRef, val currentUser: User, sessionId: Long)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
  import context.{ system, become, dispatcher }

  implicit val timeout = Timeout(5.seconds)

  val counterId = s"{currentUser.authId}"

  val randomIds = new ConcurrentLinkedHashMap.Builder[Long, Boolean]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  def receive: Actor.Receive = {
    case RpcProtocol.Request(p, messageId,
      RequestSendMessage(
        uid, accessHash, randomId, useAesKey, aesMessage, messages
        )) =>
      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          handleActor ! PackageToSend(p.replyWith(messageId,
            RpcResponseBox(
              messageId,
              Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false))).right)
        case None =>
          randomIds.put(randomId, true)
          handleRequestSendMessage(p, messageId)(uid, accessHash, randomId, useAesKey, aesMessage, messages) onFailure {
            case err =>
              handleActor ! PackageToSend(p.replyWith(messageId,
                // TODO: hide error message
                RpcResponseBox(
                  messageId,
                  Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true))).right)
              randomIds.remove(randomId)
              log.error(s"Failed to send message ${err}")
          }
      }
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
    messages: Seq[EncryptedMessage]): Future[Unit] = {
    // TODO: check accessHash SA-21

    def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long]] = {
      log.debug(s"Resolving authId for ${uid} ${publicKeyHash}")
      authIds.get((uid, publicKeyHash)) match {
        case Some(f) =>
          log.debug(s"Resolved(cache) authId for ${uid} ${publicKeyHash}")
          f
        case None =>
          val f = UserPublicKeyRecord.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
          authIds.put((uid, publicKeyHash), f)
          f onSuccess { case _ => log.debug(s"Resolved authId for ${uid} ${publicKeyHash}") }
          f
      }
    }

    @inline
    def pushUpdates(): Unit = {
      messages map { message =>
        authIdFor(message.uid, message.publicKeyHash) onComplete {
          case Success(Some(authId)) =>
            log.info(s"Pushing message ${message}")
            updatesBrokerRegion ! NewUpdateEvent(authId, NewMessage(currentUser.uid, uid, message, aesMessage))
          case x => log.error(s"Cannot find authId for uid=${message.uid} publicKeyHash=${message.publicKeyHash} ${x}")
        }
      }
    }

    log.debug("Getting UserRecord")
    val fdestUserEntity = UserRecord.getEntity(uid)

    fdestUserEntity map {
      case Some(destUserEntity) =>
        log.debug("Got UserRecord")
        val updatesDestUserId = destUserEntity.uid
        val updatesDestPublicKeyHash = destUserEntity.publicKeyHash

        pushUpdates()

        // FIXME: handle failures (retry or error, should not break seq)
        for {
          s <- ask(
            updatesBrokerRegion, NewUpdateEvent(currentUser.authId, NewMessageSent(randomId))).mapTo[UpdatesBroker.StrictState]
        } yield {
          log.debug("Replying")
          val rsp = ResponseSendMessage(mid = s._2, seq = s._1, state = s._3)
          handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
        }
      case None =>
        throw new RuntimeException("Destination user not found")
    }
  }
}
