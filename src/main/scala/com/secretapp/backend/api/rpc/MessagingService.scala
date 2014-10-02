package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse }
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.update.{ GroupInvite, MessageReceived, MessageRead }
import com.secretapp.backend.data.models.{ GroupChat, User }
import com.secretapp.backend.persist.{ UserPublicKeyRecord, UserRecord, GroupChatRecord }
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.common.RandomService
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class MessagingServiceActor(val updatesBrokerRegion: ActorRef, val socialBrokerRegion: ActorRef, val currentUser: User)(implicit val session: CSession) extends Actor with ActorLogging with MessagingService {
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

    case RpcProtocol.Request(RequestCreateChat(randomId, title, keyHash, publicKey, invites)) =>
      val replyTo = sender()
      handleRequestCreateChat(randomId, title, keyHash, publicKey, invites) pipeTo replyTo

    case RpcProtocol.Request(RequestSendGroupMessage(chatId, accessHash, randomId, message, selfMessage)) =>
      val replyTo = sender()
      handleRequestSendGroupMessage(chatId, accessHash, randomId, message, selfMessage) pipeTo replyTo

    case RpcProtocol.Request(RequestSendMessage(uid, accessHash, randomId, message, selfMessage)) =>
      val replyTo = sender()

      Option(randomIds.get(randomId)) match {
        case Some(_) =>
          replyTo ! Error(409, "MESSAGE_ALREADY_SENT", "Message with the same randomId has been already sent.", false)
        case None =>
          randomIds.put(randomId, true)
          val f = handleRequestSendMessage(uid, accessHash, randomId, message, selfMessage) map { res =>
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

sealed trait MessagingService extends RandomService {
  self: MessagingServiceActor =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  // Stores (userId, publicKeyHash) -> authId associations
  // TODO: migrate to ConcurrentLinkedHashMap
  val authIds = new TrieMap[(Int, Long), Future[Option[Long]]]

  // Caches userId -> accessHash associations
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Map[Long, User]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  protected def handleRequestCreateChat(
    randomId: Long,
    title: String,
    keyHash: BitVector,
    publicKey: BitVector,
    invites: immutable.Seq[InviteUser]
  ): Future[RpcResponse] = {
    val id = rand.nextInt
    val chat = GroupChat(id, currentUser.uid, rand.nextLong, title, keyHash, publicKey)

    GroupChatRecord.insertEntity(chat) flatMap { _ =>
      Future.sequence(invites.toVector map createChatUserInvites(chat)) map { ei =>
        val (errors, invites) = ei.separate
        if (errors.length > 0) {
          errors.head
        } else {
          invites.flatten foreach {
            case (authId, invite) =>
              updatesBrokerRegion ! NewUpdatePush(authId, invite)
          }
          Ok(ResponseCreateChat(chat.id, chat.accessHash, 0, None))
        }
      }
    }
  }

  protected def createChatUserInvites(chat: GroupChat)(invite: InviteUser): Future[Error \/ Vector[(Long, GroupInvite)]] = {
    getUsers(invite.uid) map {
      case users if users.isEmpty =>
        Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left
      case users =>
        val (_, checkUser) = users.head

        if (checkUser.accessHash(currentUser.authId) == invite.accessHash) {
          val jobsOpts = invite.keys flatMap { message =>
            message.keys map ((message.message, _))
          } map {
            case (message, key) =>
              users.get(key.keyHash) map ((_, message, key))
          }

          val optInvites: Option[Vector[(Long, GroupInvite)]] = jobsOpts.toVector.sequence map { jobs =>
            jobs map {
              case (user, message, key) =>
                (
                  user.authId,
                  GroupInvite(
                    chatId = chat.id,
                    accessHash = chat.accessHash,
                    title = chat.title,
                    users = immutable.Seq.empty,
                    keyHash = user.publicKeyHash,
                    aesEncryptedKey = key.aesEncryptedKey,
                    message = message
                  )
                )
            }
          }
          optInvites map (invites => invites.right) getOrElse (Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true).left)
        } else {
          Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false).left
        }
    }
  }

  protected def handleRequestSendGroupMessage(chatId: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedMessage,
    selfMessage: Option[EncryptedMessage]): Future[RpcResponse] = ???

  protected def getUsers(uid: Int): Future[Map[Long, User]] = {
    Option(usersCache.get(uid)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        UserRecord.getEntities(uid) map (
          _ map {user => (user.publicKeyHash, user) } toMap
        )
    }
  }

  protected def handleRequestMessageReceived(uid: Int, randomId: Long, accessHash: Long): Future[RpcResponse] = {
    getUsers(uid) flatMap {
      case users if users.isEmpty =>
        Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
      case users =>
        val (authId, user) = users.head

        if (user.accessHash(currentUser.authId) == accessHash) {
          users map { u =>
            updatesBrokerRegion ! NewUpdatePush(authId, MessageReceived(currentUser.uid, randomId))
          }
          for {
            seq <- ask(updatesBrokerRegion, UpdatesBroker.GetSeq(currentUser.authId)).mapTo[Int]
          } yield Ok(ResponseVoid())
        } else {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        }
    }
  }

  // TODO: DRY
  protected def handleRequestMessageRead(uid: Int, randomId: Long, accessHash: Long): Future[RpcResponse] = {
    getUsers(uid) flatMap {
      case users if users.isEmpty =>
        Future.successful(Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true))
      case users =>
        val (authId, user) = users.head

        if (user.accessHash(currentUser.authId) == accessHash) {
          users map { u =>
            updatesBrokerRegion ! NewUpdatePush(authId, MessageRead(currentUser.uid, randomId))
          }
          for {
            seq <- ask(updatesBrokerRegion, UpdatesBroker.GetSeq(currentUser.authId)).mapTo[Int]
          } yield Ok(ResponseVoid())
        } else {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        }
    }
  }

  protected def handleRequestSendMessage(uid: Int,
    accessHash: Long,
    randomId: Long,
    message: EncryptedMessage,
    selfMessage: Option[EncryptedMessage]
  ): Future[RpcResponse] = {
    // TODO: check accessHash SA-21

    @inline
    def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long]] = {
      log.debug(s"Resolving authId for ${uid} ${publicKeyHash}")
      authIds.get((uid, publicKeyHash)) match {
        case Some(f) =>
          f onSuccess {
            case authId =>
              log.debug(s"Resolved(cache) authId $authId for $uid $publicKeyHash")
          }
          f
        case None =>
          val f = UserPublicKeyRecord.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
          authIds.put((uid, publicKeyHash), f)
          f onSuccess { case authId => log.debug(s"Resolved authId $authId for $uid $publicKeyHash") }
          f
      }
    }

    @inline
    def pushUpdates(): Unit = {
      val pairs = selfMessage match {
        case Some(realSelfMessage) =>
          Set(
            (uid, message),
            (currentUser.uid, realSelfMessage)
          )
        case None => Set((uid, message))
      }

      pairs map { case (targetUid, encMessage) =>
        message.keys map { key =>
          authIdFor(targetUid, key.keyHash) onComplete {
            case Success(Some(targetAuthId)) =>
              log.info(s"Pushing to authId $targetAuthId message ${encMessage}")
              updatesBrokerRegion ! NewUpdateEvent(targetAuthId, NewMessage(currentUser.uid, targetUid, key.keyHash, key.aesEncryptedKey, encMessage.message))
            case x =>
              throw new Exception(s"Cannot find authId for uid=${targetUid} publicKeyHash=${key.keyHash}")
          }
        }
      }
    }

    UserRecord.getEntity(uid) flatMap {
      case Some(destUserEntity) =>
        val updatesDestUserId = destUserEntity.uid
        val updatesDestPublicKeyHash = destUserEntity.publicKeyHash

        // Record relation between receiver authId and sender uid
        log.debug(s"Recording relation uid=${uid} -> uid=${currentUser.uid}")
        socialBrokerRegion ! SocialProtocol.SocialMessageBox(
          uid, SocialProtocol.RelationsNoted(Set(currentUser.uid)))

        pushUpdates()

        // FIXME: handle failures (retry or error, should not break seq)
        for {
          s <- ask(
            updatesBrokerRegion, NewUpdateEvent(currentUser.authId, NewMessageSent(uid, randomId))).mapTo[UpdatesBroker.StrictState]
        } yield {
          log.debug("Replying")
          val rsp = updateProto.ResponseSeq(seq = s._1, state = Some(s._2))
          Ok(rsp)
        }
      case None =>
       Future.successful(Error(400, "INTERNAL_ERROR", "Destination user not found", true))
    }
  }
}
