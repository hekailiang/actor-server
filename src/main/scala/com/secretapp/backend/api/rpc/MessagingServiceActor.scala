package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.message.rpc.{ Error, RpcResponse }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.models.User
import com.secretapp.backend.persist.FileRecord
import scala.concurrent.duration._

class MessagingServiceActor(
  val updatesBrokerRegion: ActorRef, val socialBrokerRegion: ActorRef,
  val fileRecord: FileRecord, val filesCounterProxy: ActorRef, val currentUser: User
)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
  import context.{ system, become, dispatcher }

  implicit val timeout = Timeout(5.seconds)

  val counterId = currentUser.authId.toString

  val randomIds = new ConcurrentLinkedHashMap.Builder[Long, Boolean]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  def receive: Actor.Receive = {
    case RpcProtocol.Request(RequestMessageReceived(uid, randomId, accessHash)) =>
      val replyTo = sender()
      handleRequestMessageReceived(uid, randomId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestMessageRead(uid, randomId, accessHash)) =>
      val replyTo = sender()
      handleRequestMessageRead(uid, randomId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestCreateGroup(randomId, title, keyHash, publicKey, invites)) =>
      val replyTo = sender()
      handleRequestCreateGroup(randomId, title, keyHash, publicKey, invites) pipeTo replyTo

    case RpcProtocol.Request(RequestInviteUsers(groupId, accessHash, randomId, groupKeyHash, broadcast)) =>
      val replyTo = sender()
      handleRequestInviteUser(groupId, accessHash, randomId, groupKeyHash, broadcast) pipeTo replyTo

    case RpcProtocol.Request(RequestLeaveGroup(groupId, accessHash)) =>
      val replyTo = sender()
      handleRequestLeaveGroup(groupId, accessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestRemoveUser(groupId, accessHash, userId, userAccessHash)) =>
      val replyTo = sender()
      handleRequestRemoveUser(groupId, accessHash, userId, userAccessHash) pipeTo replyTo

    case RpcProtocol.Request(RequestEditGroupTitle(groupId, accessHash, title)) =>
      val replyTo = sender()
      handleRequestEditGroupTitle(groupId, accessHash, title) pipeTo replyTo

    case RpcProtocol.Request(RequestEditGroupAvatar(groupId, accessHash, avatar)) =>
      val replyTo = sender()
      handleRequestEditGroupAvatar(groupId, accessHash, avatar) pipeTo replyTo

    case RpcProtocol.Request(RequestSendGroupMessage(groupId, accessHash, randomId, message)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          randomIds.put(randomId, true)
          val f = handleRequestSendGroupMessage(groupId, accessHash, randomId, message) map { res =>
            replyTo ! res
          }

          f onFailure {
            case err =>
              replyTo ! Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true)
              randomIds.remove(randomId)
              log.error(s"Failed to send message ${err}")
          }
      }

    case RpcProtocol.Request(RequestSendMessage(uid, accessHash, randomId, message)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          val f = handleRequestSendMessage(uid, accessHash, randomId, message) map { res =>
            replyTo ! res
          }
          f onFailure {
            case err =>
              replyTo ! Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true)
              randomIds.remove(randomId)
              log.error(s"Failed to send message ${err}")
          }
      }
  }
}
