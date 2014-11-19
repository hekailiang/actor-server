package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseAvatarChanged, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.helpers.{ GroupHelpers, UserHelpers }
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait HistoryHandlers extends RandomService with UserHelpers with GroupHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  val handleHistory: RequestMatcher = {
    case RequestLoadHistory(peer, randomId, message) =>
      handleRequestLoadHistory(peer, randomId, message)
    case RequestLoadDialogs(startDate, limit) =>
      handleRequestLoadDialogs(startDate, limit)
    case RequestMessageDelete(outPeer, randomIds) =>
      handleRequestMessageDelete(outPeer, randomIds)
  }

  protected def handleRequestMessageDelete(
    outPeer: struct.OutPeer,
    randomIds: immutable.Seq[Long]
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      markMessageDeleted(currentUser.uid, outPeer.asPeer, randomIds)
      val authIdsF = outPeer.typ match {
        case struct.PeerType.Group =>
          getGroupUserAuthIds(outPeer.id)
        case struct.PeerType.Private =>
          val ownAuthIdsF = getAuthIds(currentUser.uid)
          val outAuthIdsF = getAuthIds(outPeer.id)

          for {
            ownAuthIds <- ownAuthIdsF
            outAuthIds <- outAuthIdsF
          } yield {
            ownAuthIds ++ outAuthIds
          }
      }

      val update = updateProto.MessageDelete(outPeer.asPeer, randomIds)

      for (authIds <- authIdsF) {
        authIds foreach (writeNewUpdate(_, update))
      }

      Future.successful(Ok(ResponseVoid()))
    }
  }

  def notImplemented = throw new Exception("Try again later")

  protected def handleRequestClearChat(
    peer: struct.OutPeer
  ): Future[RpcResponse] = notImplemented

  protected def handleRequestDeleteChat(
    peer: struct.OutPeer
  ): Future[RpcResponse] = notImplemented

  // TODO: refactor
  protected def handleRequestLoadDialogs(
    startDate: Long,
    limit: Int
  ): Future[RpcResponse] = {
    persist.Dialog.fetchDialogs(currentUser.uid, startDate, limit) flatMap { dialogs =>
      val fullDialogsWithUnreadZero = dialogs.foldLeft(Vector.empty[Dialog]) {
        case (res, persist.DialogMeta(peer, senderUserId, randomId, date, message)) =>
          if (res.isEmpty)
            res :+ Dialog(
              peer = peer,
              unreadCount = 0,
              sortDate = date,
              senderUserId = senderUserId,
              randomId = randomId,
              date = date,
              message = message
            )
          else {
            val sortDate = if (date != res.last.date)
              date
            else
              date + 1

            res :+ Dialog(
              peer = peer,
              unreadCount = 0,
              sortDate = sortDate,
              senderUserId = senderUserId,
              randomId = randomId,
              date = date,
              message = message
            )
          }

      }

      val fullDialogsF = Future.sequence(
        fullDialogsWithUnreadZero map { dialog =>
          persist.DialogUnreadCounter.getCount(currentUser.uid, dialog.peer) map { count =>
            dialog.copy(unreadCount = count.toInt)
          }
        }
      )

      val (usersFutures, groupsFutures) = fullDialogsWithUnreadZero.foldLeft((Vector.empty[Future[Option[struct.User]]], Vector.empty[Future[Option[struct.Group]]])) {
        case (res, dialog) =>
          dialog.peer.typ match {
            case struct.PeerType.Private =>
              res.copy(
                _1 = res._1 :+ getUserStruct(dialog.peer.id, currentUser.authId)
              )
            case struct.PeerType.Group =>
              res.copy(
                _2 = res._2 :+ getGroupStruct(dialog.peer.id, currentUser.uid)
              )
          }
      }

      for {
        dialogs <- fullDialogsF
        users <- Future.sequence(usersFutures)
        groups <- Future.sequence(groupsFutures)
      } yield {
        Ok(ResponseLoadDialogs(
          groups = groups.flatten,
          users = users.flatten,
          dialogs = dialogs
        ))
      }
    }
  }

  protected def handleRequestLoadHistory(
    outPeer: struct.OutPeer,
    startDate: Long,
    limit: Int
  ): Future[RpcResponse] = {
    withOutPeer(outPeer, currentUser) {
      persist.HistoryMessage.fetchByPeer(currentUser.uid, outPeer.asPeer, startDate, limit) flatMap { messages =>
        val userIds = messages.foldLeft(Set.empty[Int]) { (res, message) =>
          if (message.senderUserId != currentUser.uid)
            res + message.senderUserId
          else
            res
        }

        val usersF = Future.sequence(userIds map (getUserStruct(_, currentUser.authId))) map (_.flatten)

        for (users <- usersF) yield {
          Ok(ResponseLoadHistory(messages.toVector, users.toVector))
        }
      }
    }
  }
}
