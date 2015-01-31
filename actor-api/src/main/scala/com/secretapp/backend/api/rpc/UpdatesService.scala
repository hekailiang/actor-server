package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Subscribe, SubscribeAck }
import akka.event.Logging
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.{ struct, update => updateProto }
import com.secretapp.backend.data.message.rpc.{ RpcResponse, Ok, update => updateRpcProto }
import com.secretapp.backend.data.message.rpc.update.{ ResponseGetDifference, DifferenceUpdate }
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
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._

class UpdatesServiceActor(
  val sessionActor: ActorRef,
  val updatesBrokerRegion: ActorRef,
  val subscribedToUpdates: Boolean,
  val currentUserId: Int,
  val currentAuthId: Long
) extends Actor with ActorLogging with UpdatesService {
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
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val differenceSize = 300

  protected def handleRequestGetDifference(
    seq: Int, state: Option[UUID]): Future[RpcResponse] = {
    subscribeToUpdates()

    for {
      seq <- getSeq(currentAuthId)
      difference <- persist.SeqUpdate.getDifference(
        currentAuthId, state, differenceSize + 1) flatMap (mkDifference(seq, state, _))
    } yield {
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

  protected def mkDifference(seq: Int, requestState: Option[UUID], allUpdates: immutable.Seq[persist.Entity[UUID, updateProto.SeqUpdateMessage]]): Future[ResponseGetDifference] = {
    val needMore = allUpdates.length > differenceSize
    val updates = if (needMore) allUpdates.take(allUpdates.length - 1) else allUpdates
    val state = if (updates.length > 0) Some(updates.last.key) else requestState

    // TODO: make emails and phones in one loop

    val usersGroupsFuture = for {
      groups <- mkGroups(updates)
      users <- mkUsers(currentAuthId, currentUserId, groups, updates)
    } yield (users, groups)

    usersGroupsFuture flatMap {
      case (users, groups) =>
        val phonesFuture = mkPhones(currentAuthId, users)
        val emailsFuture = mkEmails(currentAuthId, users)

        for {
          phones <- mkPhones(currentAuthId, users)
          emails <- mkEmails(currentAuthId, users)
        } yield {
          ResponseGetDifference(
            seq,
            state,
            users,
            groups,
            phones,
            emails,
            updates map { u => DifferenceUpdate(u.value) },
            needMore
          )
        }
    }
  }

  protected def mkPhones(authId: Long, users: immutable.Vector[struct.User]): Future[immutable.Vector[struct.Phone]] = {
    val phoneModelsFuture = Future.sequence(
      users map ( u => Future.sequence(u.phoneIds map (persist.UserPhone.findByUserIdAndId(u.uid, _))))
    ) map (_.flatten.flatten)

    for (phoneModels <- phoneModelsFuture) yield {
      phoneModels map (struct.Phone.fromModel(authId, _)) toVector
    }
  }

  protected def mkEmails(authId: Long, users: immutable.Vector[struct.User]): Future[immutable.Vector[struct.Email]] = {
    val emailModelsFuture = Future.sequence(
      users map { u => persist.UserEmail.findAllByUserId(u.uid) }
    ) map (_.flatten)

    for (emailModels <- emailModelsFuture) yield {
      emailModels map (struct.Email.fromModel(authId, _)) toVector
    }
  }

  protected def mkUsers(authId: Long, userId: Int, groups: immutable.Seq[struct.Group], updates: immutable.Seq[persist.Entity[UUID, updateProto.SeqUpdateMessage]]): Future[immutable.Vector[struct.User]] = {
    if (updates.length > 0) {
      val userIds: immutable.Set[Int] = (updates map (_.value.userIds) reduceLeft ((x, y) => x ++ y)) ++ (groups flatMap (_.members map (_.id)))
      Future.sequence(userIds.map(getUserStruct(_, authId, userId)).toVector) map (_.flatten)
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
  protected def getState(authId: Long): Future[(Int, Option[UUID])] = {
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
