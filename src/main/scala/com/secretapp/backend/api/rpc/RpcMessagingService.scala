package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.UpdatesProtocol
import com.secretapp.backend.api.{ PackageManagerService, UpdatesManager, RpcService, CounterProtocol, CounterState }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.data.models.SessionId
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.update.MessageCodec
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scodec.bits._
import scalaz._
import Scalaz._

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
              ask(updatesManager, UpdatesProtocol.NewUpdate(destUserEntity.key, update))
                .mapTo[(UpdatesManager.State, UUID)]
            }
          }
          // FIXME: handle failures (retry or error, should not break seq)
          Future.sequence(fupdateInserts) map { _ =>
            // TODO: Create UpdateMessageSent update
            // sendReply(p.replyWith(messageId, RpcResponseBox(messageId, /* reply here */)))
          }
        }
      case None =>
        throw new RuntimeException("Destination user not found")
    }
  }
}
