package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.helpers._
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.util.{ AvatarUtils }
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait MessagingHandlers extends RandomService with UserHelpers with GroupHelpers with PeerHelpers with UpdatesHelpers with HistoryHelpers {
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
      socialBrokerRegion ! SocialProtocol.SocialMessageBox(
        currentUser.uid, SocialProtocol.RelationsNoted(Set(outPeer.id)))

      val date = System.currentTimeMillis()

      val selfUpdateFuture = outPeer.typ match {
        case struct.PeerType.Private =>
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

          writeHistoryMessage(
            userId = currentUser.uid,
            peer = outPeer.asPeer,
            date = date,
            randomId = randomId,
            senderUserId = currentUser.uid,
            message = message
          )

          writeHistoryMessage(
            userId = outPeer.id,
            peer = struct.Peer.privat(currentUser.uid),
            date = date,
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
        case struct.PeerType.Group =>
          getGroupUserIdsWithAuthIds(outPeer.id) map { userIdsAuthIds =>
            val (userIds, authIds) = userIdsAuthIds.foldLeft((Vector.empty[Int], Vector.empty[Long])) {
              case (res, (userId, userAuthIds)) =>
                res.copy(
                  _1 = res._1 :+ userId,
                  _2 = res._2 ++ userAuthIds
                )
            }

            userIds foreach { userId =>
              writeHistoryMessage(
                userId = userId,
                peer = outPeer.asPeer,
                date = date,
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

      selfUpdateFuture flatMap {
        withNewUpdateState(
          currentUser.authId,
          _
        ) { s =>
          val rsp = ResponseSeqDate(seq = s._1, state = Some(s._2), date)
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

    if (outPeer.typ == struct.PeerType.Group) {
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
        case struct.PeerType.Private =>
          for {
            authIds <- getAuthIds(outPeer.id)
          } yield {
            val update = updateProto.EncryptedReceived(struct.Peer.privat(currentUser.uid), randomId, receivedDate)
            authIds foreach (writeNewUpdate(_, update))
          }
          Future.successful(Ok(ResponseVoid()))
        case struct.PeerType.Group =>
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
        case struct.PeerType.Private =>
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
        case struct.PeerType.Group =>
          Future.successful(Error(400, "NOT_IMPLEMENTED", "Group encrypted messaging is not implemented yet.", false))
      }
    }
  }

  protected def handleRequestMessageReceived(
    outPeer: struct.OutPeer,
    date: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val receivedDate = System.currentTimeMillis()

      val (authIdsF, update) = outPeer.typ match {
        case struct.PeerType.Group =>
          val update = updateProto.MessageReceived(struct.Peer.group(outPeer.id), date, receivedDate)

          val authIdsF = for (userIdsAuthIds <- getGroupUserIdsWithAuthIds(outPeer.id)) yield {
            userIdsAuthIds.foldLeft(Vector.empty[Long]) {
              case (res, (userId, authIds)) =>
                if (userId != currentUser.uid)
                  res ++ authIds
                else
                  res
            }
          }

          (
            authIdsF,
            updateProto.MessageReceived(struct.Peer.group(outPeer.id), date, receivedDate)
          )
        case struct.PeerType.Private =>
          (
            getAuthIds(outPeer.id),
            updateProto.MessageReceived(struct.Peer.privat(currentUser.uid), date, receivedDate)
          )
      }

      for (authIds <- authIdsF) {
        authIds foreach (writeNewUpdate(_, update))
      }

      Future.successful(Ok(ResponseVoid()))
    }
  }

  protected def handleRequestMessageRead(
    outPeer: struct.OutPeer,
    date: Long
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      val readDate = System.currentTimeMillis()

      val authIdsUpdatesF = outPeer.typ match {
        case struct.PeerType.Group =>
          getGroupUserIdsWithAuthIds(outPeer.id) map { pairs =>
            pairs.foldLeft(Vector.empty[(Long, updateProto.SeqUpdateMessage)]) {
              case (res, (userId, authIds)) =>
                if (userId != currentUser.uid)
                  res ++ (
                    authIds map ((_, updateProto.MessageRead(struct.Peer.group(outPeer.id), date, readDate)))
                  )
                else
                  res ++ (
                    authIds map ((_, updateProto.MessageReadByMe(struct.Peer.group(outPeer.id), date)))
                  )
            }
          }
        case struct.PeerType.Private =>
          markMessageRead(currentUser.uid, outPeer.asPeer, date)

          val outAuthIdsUpdatesF = for (authIds <- getAuthIds(outPeer.id)) yield {
            authIds map ((_, updateProto.MessageRead(struct.Peer.privat(currentUser.uid), date, readDate)))
          }

          val ownAuthIdsUpdatesF = for (authIds <- getAuthIds(currentUser.uid)) yield {
            authIds map ((_, updateProto.MessageReadByMe(outPeer.asPeer, date)))
          }

          for {
            out <- outAuthIdsUpdatesF
            own <- ownAuthIdsUpdatesF
          } yield (out ++ own)
      }

      for (authIdsUpdates <- authIdsUpdatesF) {
        authIdsUpdates foreach (writeNewUpdate _ tupled)
      }

      Future.successful(Ok(ResponseVoid()))
    }
  }
}
