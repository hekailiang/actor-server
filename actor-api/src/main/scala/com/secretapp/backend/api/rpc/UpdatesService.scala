package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Subscribe, SubscribeAck }
import akka.event.Logging
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.{ struct, update => updateProto }
import com.secretapp.backend.data.message.rpc.{ RpcResponse, Ok, update => updateRpcProto }
import com.secretapp.backend.data.message.rpc.update.{ Difference, DifferenceUpdate }
import com.secretapp.backend.data.message.update.SeqUpdate
import com.secretapp.backend.helpers._
import com.secretapp.backend.models
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.session.SessionProtocol
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.{ uuid => uuidCodec }

class UpdatesServiceActor(
  val sessionActor: ActorRef, val updatesBrokerRegion: ActorRef, val subscribedToUpdates: Boolean,
  val currentUserId: Int, val currentAuthId: Long
)(implicit val session: CSession) extends Actor with ActorLogging with UpdatesService {
  import context.dispatcher

  def receive = {
    case RpcProtocol.Request(updateRpcProto.RequestGetDifference(seq, state)) =>
      handleRequestGetDifference(seq, state) pipeTo sender
    case RpcProtocol.Request(updateRpcProto.RequestGetState()) =>
      handleRequestGetState() pipeTo sender
  }
}

sealed trait UpdatesService extends UserHelpers with GroupHelpers {
  self: UpdatesServiceActor =>

  import context._
  implicit val timeout = Timeout(5.seconds)

  private val differenceSize = 300

  protected def handleRequestGetDifference(
    seq: Int, state: Option[UUID]): Future[RpcResponse] = {
    subscribeToUpdates()

    for {
      seq <- getSeq(currentAuthId)
      difference <- persist.SeqUpdate.getDifference(
        currentAuthId, state, differenceSize + 1) flatMap (mkDifference(seq, state, _))
    } yield {
      //handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(difference))).right)]
      Ok(difference)
    }
  }

  protected def handleRequestGetState(): Future[RpcResponse] = {
    subscribeToUpdates()

    val f = getState(currentAuthId)

    f map {
      case (seq, muuid) =>
        val rsp = updateRpcProto.ResponseSeq(seq, muuid)
        Ok(rsp)
    }
  }

  protected def mkDifference(seq: Int, requestState: Option[UUID], allUpdates: immutable.Seq[persist.Entity[UUID, updateProto.SeqUpdateMessage]]): Future[Difference] = {
    val needMore = allUpdates.length > differenceSize
    val updates = if (needMore) allUpdates.take(allUpdates.length - 1) else allUpdates
    val state = if (updates.length > 0) Some(updates.last.key) else requestState

    val usersFuture = mkUsers(currentAuthId, updates)
    val groupsFuture = mkGroups(updates)

    for {
      users <- usersFuture
      groups <- groupsFuture
    } yield {
      Difference(seq, state, users, groups,
        updates map { u => DifferenceUpdate(u.value) }, needMore)
    }
  }

  protected def mkUsers(authId: Long, updates: immutable.Seq[persist.Entity[UUID, updateProto.SeqUpdateMessage]]): Future[immutable.Vector[struct.User]] = {
    if (updates.length > 0) {
      val userIds = updates map (_.value.userIds) reduceLeft ((x, y) => x ++ y)
      Future.sequence(userIds.map(getUserStruct(_, authId)).toVector) map (_.flatten)
    } else { Future.successful(Vector.empty) }
  }

  protected def mkGroups(updates: immutable.Seq[persist.Entity[UUID, updateProto.SeqUpdateMessage]]): Future[immutable.Vector[struct.Group]] = {
    if (updates.length > 0) {
      val groupIds = updates map (_.value.groupIds) reduceLeft ((x, y) => x ++ y)
      Future.sequence(groupIds.map(getGroupStruct(_, currentUserId)).toVector) map (_.flatten)
    } else { Future.successful(Vector.empty) }
  }

  protected def subscribeToUpdates() = {
    if (!subscribedToUpdates) {
      sessionActor ! SessionProtocol.SubscribeToUpdates
    }
  }

  @inline
  protected def getState(authId: Long)(implicit session: CSession): Future[(Int, Option[UUID])] = {
    val fseq = getSeq(authId)
    val fstate = persist.SeqUpdate.getState(authId)
    for {
      seq <- fseq
      muuid <- fstate
    } yield (seq, muuid)
  }

  private def getSeq(authId: Long): Future[Int] = {
    ask(updatesBrokerRegion, UpdatesBroker.GetSeq(authId)).mapTo[Int]
  }
}
