package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.event.Logging
import akka.util.Timeout
import akka.pattern.ask
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.update.Difference
import com.secretapp.backend.data.message.rpc.update.DifferenceUpdate
import com.secretapp.backend.data.message.rpc.{ update => updateRpcProto }
import com.secretapp.backend.data.message.update.CommonUpdate
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.rpc.RpcCommon
import java.util.UUID
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.collection.immutable
import scala.concurrent.duration._
import scodec.codecs.{ uuid => uuidCodec }
import scodec.bits._
import scalaz._
import akka.pattern.ask
import Scalaz._

class UpdatesServiceActor(val handleActor: ActorRef, val updatesBrokerRegion: ActorRef, val uid: Int, val authId: Long)(implicit val session: CSession) extends Actor with ActorLogging with UpdatesService with PackageCommon with RpcCommon {
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(authId)

  def receive = {
    case RpcProtocol.Request(p, messageId, updateRpcProto.RequestGetDifference(seq, state)) =>
      handleRequestGetDifference(p, messageId)(seq, state)
    case RpcProtocol.Request(p, messageId, updateRpcProto.RequestGetState()) =>
      handleRequestGetState(p, messageId)
  }
}

sealed trait UpdatesService {
  self: UpdatesServiceActor =>

  import context._
  implicit val timeout = Timeout(5.seconds)

  private val updatesPusher = context.actorOf(Props(new PusherActor(handleActor)))
  private var subscribedToUpdates = false
  private val differenceSize = 500

  protected def handleRequestGetDifference(p: Package, messageId: Long)(
    seq: Int, state: Option[UUID]
  ) = {
    subscribeToUpdates(authId)

    val res = for {
      seq <- getSeq(authId)
      difference <- CommonUpdateRecord.getDifference(
        authId, state, differenceSize + 1
      ) flatMap (mkDifference(seq, _))
    } yield {
      //handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(difference))).right)]
      difference.right
    }

    val f = res recover {
      case e: Throwable =>
        log.error(s"Error getting difference ${e} ${Logging.stackTraceFor(e)}")
        internalError.left
    }
    sendRpcResult(p, messageId)(f)
  }

  protected def handleRequestGetState(p: Package, messageId: Long) = {
    subscribeToUpdates(authId)

    val f = getState(authId)

    f onComplete {
      case Success((seq, muuid)) =>
        val rsp = updateRpcProto.State(seq, muuid)
        handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
      case Failure(err) =>
        // TODO: Handle failure
        throw new Exception("Failed to get state")
    }
  }

  protected def mkDifference(seq: Int, allUpdates: immutable.Seq[Entity[UUID, updateProto.CommonUpdateMessage]]): Future[Difference] = {
    val needMore = allUpdates.length > differenceSize
    val updates = if (needMore) allUpdates.tail else allUpdates
    val users = mkUsers(authId, updates)
    val state = if (updates.length > 0) Some(updates.last.key) else None
    users map (Difference(seq, state, _,
      updates map {u => DifferenceUpdate(u.value)}, needMore))
  }

  protected def mkUsers(authId: Long, updates: immutable.Seq[Entity[UUID, updateProto.CommonUpdateMessage]]): Future[immutable.Vector[struct.User]] = {
    @inline
    def getUserStruct(uid: Int): Future[Option[struct.User]] = {
      UserRecord.getEntity(uid) map (_ map (_.toStruct(authId)))
    }
    if (updates.length > 0) {
      val userIds = updates map (_.value.userIds) reduceLeft ((x, y) => x ++ y)
      Future.sequence(userIds.map(getUserStruct(_)).toVector) map (_.flatten)
    } else { Future.successful(Vector.empty) }
  }

  protected def subscribeToUpdates(authId: Long) = {
    if (!subscribedToUpdates) {
      log.info("Subscribing to updates authId={}", authId)
      mediator ! Subscribe(UpdatesBroker.topicFor(authId), updatesPusher)
    }
  }

  @inline
  protected def getState(authId: Long)(implicit session: CSession): Future[(Int, Option[UUID])] = {
    for {
      seq <- getSeq(authId)
      muuid <- CommonUpdateRecord.getState(authId)
    } yield {
      (seq, muuid)
    }
  }

  private def getSeq(authId: Long): Future[Int] = {
    ask(updatesBrokerRegion, UpdatesBroker.GetSeq(authId)).mapTo[Int]
  }
}

sealed class PusherActor(handleActor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case (seq: Int, state: UUID, u: updateProto.CommonUpdateMessage) =>
      log.info(s"Pushing update to session ${u}")
      val upd = CommonUpdate(seq, uuidCodec.encode(state).toOption.get, u)
      val ub = UpdateBox(upd)
      handleActor ! UpdateBoxToSend(ub)
    case u =>
      log.error(s"Unknown update in topic ${u}")
  }
}
