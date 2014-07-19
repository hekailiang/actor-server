package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.api.{ CounterState, CounterProtocol, UpdatesProtocol, UpdatesManager}
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.{update => updateProto, _}
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist._
import com.secretapp.backend.services._
import com.secretapp.backend.services.transport._
import java.util.UUID
import scala.concurrent.Future
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait RpcMessagingService {
  this: RpcService with PackageManagerService with Actor =>
  import context._

  //val updatesManager = context.actorOf(Props(new UpdatesManager(sessionId)))
  val messagesCounter = context.actorSelection("/user/message-counter")

  protected def getOrCreateUpdatesManager(keyHash: Long): Future[ActorRef] = {
    val selection = context.actorSelection(s"/user/update-manager/${keyHash.toString}")
    selection.resolveOne recover {
      case e: ActorNotFound =>
        system.actorOf(Props(new UpdatesManager(keyHash)), s"update-manager/${keyHash.toString}")
    }
  }

  def handleRequestSendMessage(p: Package, messageId: Long, currentUser: (Long, User))(uid: Int,
    accessHash: Long,
    randomId: Long,
    useAesKey: Boolean,
    aesMessage: Option[BitVector],
    messages: Seq[EncryptedMessage]) = {
    // TODO: check accessHash SA-21

    @inline
    def mkUpdates(destUserEntity: Entity[Int, User], mid: Int): Seq[updateProto.Message] = {
      val Entity(destUID, destUser) = destUserEntity
      messages map { message =>
        // TODO: check conformity of keyHash to uid
        updateProto.Message(
          senderUID = currentUser._1.toInt,
          destUID = destUID,
          mid = mid,
          keyHash = message.keyHash,
          useAesKey = useAesKey,
          aesKey = message.aesEncryptedKey,
          message = useAesKey match {
            case true => aesMessage.get
            case false => message.message.get
          }
        )
      }
    }

    val fmid = ask(messagesCounter, CounterProtocol.GetNext).mapTo[CounterState] map (_.counter)
    val fdestUserEntity = UserRecord.getEntity(currentUser._1.toInt)

    fdestUserEntity map {
      case Some(destUserEntity) =>
        val fupdates = for {
          mid <- fmid
        } yield {
          val fupdateInserts = mkUpdates(destUserEntity, mid.toInt) map { update =>
            getOrCreateUpdatesManager(update.keyHash) flatMap { updatesManager =>
              ask(updatesManager, UpdatesProtocol.NewUpdate(update))
                .mapTo[(UpdatesManager.State, UUID)]
            }
          }
          // FIXME: handle failures (retry or error, should not break seq)
          for {
            // current user's updates manager
            updatesManager <- getOrCreateUpdatesManager(currentUser._2.publicKeyHash)
            _ <- Future.sequence(fupdateInserts)
            s <- ask(updatesManager, UpdatesProtocol.NewUpdate(updateProto.MessageSent(mid.toInt, randomId))).mapTo[(UpdatesManager.State, UUID)]
          } yield {
            val rsp = ResponseSendMessage(mid.toInt, s._1.seq, uuid.encode(s._2).toOption.get)
            // TODO: Create UpdateMessageSent update
            sendReply(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
          }
        }
      case None =>
        throw new RuntimeException("Destination user not found")
    }
  }
}
