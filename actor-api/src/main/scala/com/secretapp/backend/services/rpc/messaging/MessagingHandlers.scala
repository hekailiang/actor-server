package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.helpers._
import com.secretapp.backend.services.common.RandomService
import org.joda.time.DateTime
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait MessagingHandlers extends RandomService with UserHelpers with GroupHelpers with PeerHelpers with UpdatesHelpers
with HistoryHelpers with SendMessagingHandlers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  val handleMessaging: RequestMatcher = {
    case RequestSendMessage(peer, randomId, message) =>
      handleRequestSendMessage(peer, randomId, message)
    case RequestSendEncryptedMessage(outPeer, randomId, encryptedMessage, keys, ownKeys) =>
      handleRequestSendEncryptedMessage(outPeer, randomId, encryptedMessage, keys, ownKeys)
    case RequestEncryptedReceived(outPeer, randomId) =>
      handleRequestEncryptedReceived(outPeer, randomId)
    case RequestEncryptedRead(outPeer, randomId) =>
      handleRequestEncryptedRead(outPeer, randomId)
    case RequestMessageReceived(outPeer, date) =>
      handleRequestMessageReceived(outPeer, date)
    case RequestMessageRead(outPeer, randomId) =>
      handleRequestMessageRead(outPeer, randomId)
  }

  protected def handleRequestSendMessage(
    outPeer: struct.OutPeer,
    randomId: Long,
    message: MessageContent
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      // Record relation between receiver authId and sender uid

      sendMessage(
        socialBrokerRegion = socialBrokerRegion,
        dialogManagerRegion = dialogManagerRegion,
        currentUser = currentUser,
        outPeer = outPeer,
        randomId = randomId,
        message = message
      ) flatMap { msg =>
        withNewUpdateState(
          currentUser.authId,
          msg
        ) { s =>
          val rsp = ResponseSeqDate(seq = s._1, state = Some(s._2), msg.date)
          Ok(rsp)
        }
      }
    }
  }

  protected def handleRequestSendEncryptedMessage(
    outPeer: struct.OutPeer,
    randomId: Long,
    encryptedMessage: BitVector,
    keys: immutable.Seq[EncryptedAESKey],
    ownKeys: immutable.Seq[EncryptedAESKey]
  ): Future[RpcResponse] = {
    val outUserId = outPeer.id
    val date = System.currentTimeMillis

    if (outPeer.typ == models.PeerType.Group) {
      throw new Exception("Encrypted group sending is not implemented yet.")
    }

    val ownPairsEFuture = fetchAuthIdsForValidKeys(currentUser.uid, ownKeys, Some(currentUser.publicKeyHash))
    val outPairsEFuture = fetchAuthIdsForValidKeys(outUserId, keys, None)

    val ownPeer = struct.Peer.privat(currentUser.uid)

    val peersAuthIdsEFuture = for {
      ownPairsE <- ownPairsEFuture
      outPairsE <- outPairsEFuture
    } yield {
      @inline def withPeer(peer: struct.Peer, pairs: Vector[(EncryptedAESKey, Long)]): Vector[(EncryptedAESKey, (struct.Peer, Long))] = {
        pairs map (p => (p._1, (peer, p._2)))
      }
      (ownPairsE map (withPeer(outPeer.asPeer, _))) +++ (outPairsE map (withPeer(ownPeer, _)))
    }

    withOutPeer(outPeer, currentUser) {
      // Record relation between receiver authId and sender uid
      socialBrokerRegion ! SocialProtocol.SocialMessageBox(
        currentUser.uid, SocialProtocol.RelationsNoted(Set(outUserId)))

      peersAuthIdsEFuture flatMap {
        case \/-(pairs) =>
          // TODO: get time from an actor with PinnedDispatcher
          val date = System.currentTimeMillis()

          pairs foreach {
            case (EncryptedAESKey(keyHash, aesEncryptedKey), (peer, authId)) =>
              writeNewUpdate(
                authId,
                updateProto.EncryptedMessage(
                  peer = peer,
                  senderUid = currentUser.uid,
                  keyHash = keyHash,
                  aesEncryptedKey = aesEncryptedKey,
                  message = encryptedMessage,
                  date = date
                )
              )
          }

          withNewUpdateState(
            currentUser.authId,
            updateProto.MessageSent(
              outPeer.asPeer,
              randomId,
              date
            )
          ) { s =>
            val rsp = ResponseSeqDate(seq = s._1, state = Some(s._2), date = date)
            Ok(rsp)
          }
        case -\/((newKeys, removedKeys, invalidKeys)) =>
          val errorData = struct.WrongKeysErrorData(newKeys.toSeq, removedKeys.toSeq, invalidKeys.toSeq)

          Future.successful(
            Error(400, "WRONG_KEYS", "", false, Some(errorData))
          )
      }
    }
  }

  protected def handleRequestEncryptedReceived(
    outPeer: struct.OutPeer,
    randomId: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val receivedDate = System.currentTimeMillis()

      outPeer.typ match {
        case models.PeerType.Private =>
          for {
            authIds <- getAuthIds(outPeer.id)
          } yield {
            val update = updateProto.EncryptedReceived(struct.Peer.privat(currentUser.uid), randomId, receivedDate)
            authIds foreach (writeNewUpdate(_, update))
          }
          Future.successful(Ok(ResponseVoid()))
        case models.PeerType.Group =>
          Future.successful(Error(400, "NOT_IMPLEMENTED", "Group encrypted messaging is not implemented yet.", false))
      }
    }
  }

  protected def handleRequestEncryptedRead(
    outPeer: struct.OutPeer,
    randomId: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val readDate = System.currentTimeMillis()

      outPeer.typ match {
        case models.PeerType.Private =>
          val peerAuthIdsFuture = getAuthIds(outPeer.id)
          val myAuthIdsFuture   = getAuthIds(currentUser.uid)

          for {
            peerAuthIds <- peerAuthIdsFuture
            myAuthIds   <- myAuthIdsFuture
          } yield {
            peerAuthIds foreach { authId =>
              updatesBrokerRegion ! NewUpdatePush(authId, updateProto.EncryptedRead(struct.Peer.privat(currentUser.uid), randomId, readDate))
            }

            myAuthIds foreach { authId =>
              updatesBrokerRegion ! NewUpdatePush(authId, updateProto.EncryptedReadByMe(outPeer.asPeer, randomId))
            }
          }
          Future.successful(Ok(ResponseVoid()))
        case models.PeerType.Group =>
          Future.successful(Error(400, "NOT_IMPLEMENTED", "Group encrypted messaging is not implemented yet.", false))
      }
    }
  }

  protected def handleRequestMessageReceived(
    outPeer: struct.OutPeer,
    date: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val dateTime = new DateTime(date)
      val receivedDate = System.currentTimeMillis

      outPeer.typ match {
        case models.PeerType.Group =>
          markInMessagesReceived(currentUser.uid, outPeer.asPeer.asModel, dateTime)

          for (userIdsAuthIds <- getGroupUserIdsWithAuthIds(outPeer.id)) {
            val update = updateProto.MessageReceived(struct.Peer.group(outPeer.id), date, receivedDate)

            userIdsAuthIds map {
              case (userId, authIds) =>
                if (userId != currentUser.uid) {
                  markOutMessagesReceived(userId, outPeer.asPeer.asModel, dateTime)

                  authIds foreach (writeNewUpdate(_, update))
                }
            }
          }
        case models.PeerType.Private =>
          val currentPeer = struct.Peer.privat(currentUser.uid)

          markOutMessagesReceived(outPeer.id, currentPeer.asModel, dateTime)
          markInMessagesReceived(currentUser.uid, outPeer.asPeer.asModel, dateTime)

          val update = updateProto.MessageReceived(currentPeer, date, receivedDate)

          for (authIds <- getAuthIds(outPeer.id)) {
            authIds foreach (writeNewUpdate(_, update))
          }
      }

      Future.successful(Ok(ResponseVoid()))
    }
  }

  protected def handleRequestMessageRead(
    outPeer: struct.OutPeer,
    date: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val dateTime = new DateTime(date)
      val readDate = System.currentTimeMillis

      outPeer.typ match {
        case models.PeerType.Group =>
          markInMessagesReceived(currentUser.uid, outPeer.asPeer.asModel, dateTime)

          for (userIdsAuthIds <- getGroupUserIdsWithAuthIds(outPeer.id)) {
            val update = updateProto.MessageRead(struct.Peer.group(outPeer.id), date, readDate)
            val selfUpdate = updateProto.MessageReadByMe(struct.Peer.group(outPeer.id), date)

            userIdsAuthIds map {
              case (userId, authIds) =>
                if (userId != currentUser.uid) {
                  markOutMessagesReceived(userId, outPeer.asPeer.asModel, dateTime)
                  authIds foreach (writeNewUpdate(_, update))
                } else {
                  authIds foreach (writeNewUpdate(_, selfUpdate))
                }
            }
          }
        case models.PeerType.Private =>
          val currentPeer = struct.Peer.privat(currentUser.uid)
          val update = updateProto.MessageRead(currentPeer, date, readDate)
          val selfUpdate = updateProto.MessageReadByMe(outPeer.asPeer, date)

          markOutMessagesRead(outPeer.id, currentPeer.asModel, dateTime)
          markInMessagesRead(currentUser.uid, outPeer.asPeer.asModel, dateTime)

          for (authIds <- getAuthIds(outPeer.id)) yield {
            authIds foreach (writeNewUpdate(_, update))
          }

          for (authIds <- getAuthIds(currentUser.uid)) yield {
            authIds foreach (writeNewUpdate(_, selfUpdate))
          }
      }

      Future.successful(Ok(ResponseVoid()))
    }
  }
}

trait SendMessagingHandlers {
  self: RandomService with UserHelpers with GroupHelpers with PeerHelpers with UpdatesHelpers with HistoryHelpers =>

  import UpdatesBroker._

  def sendMessage(socialBrokerRegion: ActorRef,
                  dialogManagerRegion: ActorRef,
                  currentUser: models.User,
                  outPeer: struct.OutPeer,
                  randomId: Long,
                  message: MessageContent)(implicit ec: ExecutionContext): Future[updateProto.MessageSent] = {
    // Record relation between receiver authId and sender uid

    socialBrokerRegion ! SocialProtocol.SocialMessageBox(
      currentUser.uid, SocialProtocol.RelationsNoted(Set(outPeer.id)))

    val dateTime = new DateTime
    val date = dateTime.getMillis

    outPeer.typ match {
      case models.PeerType.Private =>
        val myAuthIdsFuture   = getAuthIds(currentUser.uid)
        val peerAuthIdsFuture = getAuthIds(outPeer.id)

        for {
          myAuthIds <- myAuthIdsFuture
          peerAuthIds <- peerAuthIdsFuture
        } yield {
          val ownUpdate = updateProto.Message(
            peer = outPeer.asPeer,
            senderUid = currentUser.uid,
            date = date,
            randomId = randomId,
            message = message
          )

          val outUpdate = updateProto.Message(
            peer = struct.Peer.privat(currentUser.uid),
            senderUid = currentUser.uid,
            date = date,
            randomId = randomId,
            message = message
          )

          (myAuthIds filterNot (_ == currentUser.authId)) foreach { authId =>
            updatesBrokerRegion ! NewUpdatePush(authId, ownUpdate)
          }

          peerAuthIds foreach { authId =>
            updatesBrokerRegion ! NewUpdatePush(authId, outUpdate)
          }
        }

        writeOutHistoryMessage(
          userId = currentUser.uid,
          peer = outPeer.asPeer.asModel,
          date = dateTime,
          randomId = randomId,
          senderUserId = currentUser.uid,
          message = message
        )

        writeInHistoryMessage(
          userId = outPeer.id,
          peer = models.Peer.privat(currentUser.uid),
          date = dateTime,
          randomId = randomId,
          senderUserId = currentUser.uid,
          message = message
        )

        Future.successful(
          updateProto.MessageSent(
            outPeer.asPeer,
            randomId,
            date
          )
        )
      case models.PeerType.Group =>
        getGroupUserIdsWithAuthIds(outPeer.id) map { userIdsAuthIds =>
          val (userIds, authIds) = userIdsAuthIds.foldLeft((Vector.empty[Int], Vector.empty[Long])) {
            case (res, (userId, userAuthIds)) =>
              res.copy(
                _1 = res._1 :+ userId,
                _2 = res._2 ++ userAuthIds
              )
          }

          writeOutHistoryMessage(
            userId = currentUser.uid,
            peer = outPeer.asPeer.asModel,
            date = dateTime,
            randomId = randomId,
            senderUserId = currentUser.uid,
            message = message
          )

          userIds foreach { userId =>
            if (userId != currentUser.uid)
              writeInHistoryMessage(
                userId = userId,
                peer = outPeer.asPeer.asModel,
                date = dateTime,
                randomId = randomId,
                senderUserId = currentUser.uid,
                message = message
              )
          }

          authIds foreach { authId =>
            if (authId != currentUser.authId) {
              writeNewUpdate(
                authId,
                updateProto.Message(
                  peer = struct.Peer.group(outPeer.id),
                  senderUid = currentUser.uid,
                  date = date,
                  randomId = randomId,
                  message = message
                )
              )
            }
          }

          updateProto.MessageSent(
            outPeer.asPeer,
            randomId,
            date
          )
        }
    }
  }
}
