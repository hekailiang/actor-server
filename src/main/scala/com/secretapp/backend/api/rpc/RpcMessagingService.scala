package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ CounterProtocol, CounterState, UpdatesManager, UpdatesProtocol }
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

  //val updatesManager = context.actorOf(Props(new UpdatesManager(sessionId)))
  val messagesCounter = context.actorSelection("/user/message-counter")

  // TODO: cache result
  protected def getOrCreateUpdatesManager(keyHash: Long): Future[ActorRef] = {
    val selection = context.actorSelection(s"/user/updates-manager-${keyHash.toString}")

    selection.resolveOne recover {
      case e: ActorNotFound =>
        println(s"Creating updates-manager-${keyHash.toString}")
        val ref = context.system.actorOf(Props(new UpdatesManager(keyHash)), s"updates-manager-${keyHash.toString}")
        println("Created")
        ref
    } andThen {
      case Failure(e) =>
        log.error("Cannot resolve updates-manager-${keyHash.toString}", e)
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
          })
      }
    }

    val fmid = ask(messagesCounter, CounterProtocol.GetNext).mapTo[CounterState] map (_.counter)
    val fdestUserEntity = UserRecord.getEntity(currentUser._1.toInt)

    fdestUserEntity map {
      case Some(destUserEntity) =>
        val fupdates = for {
          mid <- fmid
        } yield {
          println("destUserGot")
          val fupdateInserts = mkUpdates(destUserEntity, mid.toInt) map { update =>
            println(s"Update ${update}")
            getOrCreateUpdatesManager(update.keyHash) flatMap { updatesManager =>
              println("sending updates", updatesManager)
              ask(updatesManager, UpdatesProtocol.NewUpdate(update))
                .mapTo[(UpdatesManager.State, UUID)]
            }
          }
          // FIXME: handle failures (retry or error, should not break seq)
          for {
            // current user's updates manager
            _ <- Future.sequence(fupdateInserts)
            updatesManager <- getOrCreateUpdatesManager(currentUser._2.publicKeyHash)
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
