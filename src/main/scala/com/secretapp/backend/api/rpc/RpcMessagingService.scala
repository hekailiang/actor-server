package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ CounterProtocol, CounterState, CounterActor, UpdatesManager, UpdatesProtocol, SharedActors }
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist._
import com.secretapp.backend.services._
import com.secretapp.backend.services.transport._
import java.util.UUID
import scala.concurrent.Future
import scala.util._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait RpcMessagingService {
  this: RpcService with PackageManagerService with Actor =>

  import context.dispatcher
  import context.system

  //val updatesManager = context.actorOf(Props(new UpdatesManager(sessionId)))
  protected def messageCounter: Future[ActorRef] = {
    SharedActors.lookup("message-counter") {
      system.actorOf(Props(new CounterActor("message")))
    }
  }

  // TODO: cache result
  protected def updatesManager(keyHash: Long): Future[ActorRef] = {
    val name = s"updates-manager-${keyHash.toString}"
    SharedActors.lookup(name) {
      system.actorOf(Props(new UpdatesManager(keyHash)), name)
    }
  }

  def handleRequestSendMessage(p: Package, messageId: Long, currentUser: User)(uid: Int,
    accessHash: Long,
    randomId: Long,
    useAesKey: Boolean,
    aesMessage: Option[BitVector],
    messages: Seq[EncryptedMessage]) = {
    // TODO: check accessHash SA-21

    @inline
    def mkUpdates(destUser: User, mid: Int): Seq[updateProto.Message] = {
      messages map { message =>
        // TODO: check conformity of keyHash to uid
        updateProto.Message(
          senderUID = currentUser.uid,
          destUID = destUser.uid,
          mid = mid,
          keyHash = message.keyHash,
          useAesKey = useAesKey,
          aesKey = message.aesEncryptedKey,
          message = useAesKey match {
            case true => aesMessage.get
            case false => message.message.get
          })
      }
    }

    val fmid = messageCounter.flatMap(ask(_, CounterProtocol.GetNext).mapTo[CounterState] map (_.counter))
    val fdestUserEntity = UserRecord.getEntity(currentUser.uid)

    fdestUserEntity map {
      case Some(destUserEntity) =>
        val fupdates = for {
          mid <- fmid
        } yield {
          println("destUserGot")
          val fupdateInserts = mkUpdates(destUserEntity, mid.toInt) map { update =>
            println(s"Update ${update}")
            updatesManager(update.keyHash) flatMap { updatesManager =>
              println("sending updates", updatesManager)
              ask(updatesManager, UpdatesProtocol.NewUpdate(update))
                .mapTo[(UpdatesManager.State, UUID)]
            }
          }
          // FIXME: handle failures (retry or error, should not break seq)
          for {
            // current user's updates manager
            _ <- Future.sequence(fupdateInserts)
            updatesManager <- updatesManager(currentUser.publicKeyHash)
            s <- ask(updatesManager, UpdatesProtocol.NewUpdate(updateProto.MessageSent(mid.toInt, randomId))).mapTo[(UpdatesManager.State, UUID)]
          } yield {
            val rsp = ResponseSendMessage(mid.toInt, s._1.seq, uuid.encode(s._2).toOption.get)
            sendReply(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
          }
        }
      case None =>
        throw new RuntimeException("Destination user not found")
    }
  }
}
