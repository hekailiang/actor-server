package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
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
import com.secretapp.backend.services.common.PackageCommon._
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

class UpdatesManager(val handleActor: ActorRef, val uid: Int, val publicKeyHash: Long, val authId: Long)(implicit val session: CSession) extends Actor with ActorLogging with UpdatesService {
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(uid, publicKeyHash)

  def receive = {
    case RpcProtocol.Request(p, messageId, updateRpcProto.RequestGetDifference(seq, state)) =>
      handleRequestGetDifference(p, messageId)(seq, state)
    case RpcProtocol.Request(p, messageId, updateRpcProto.RequestGetState()) =>
      handleRequestGetState(p, messageId)
  }
}

sealed trait UpdatesService {
  self: UpdatesManager =>

  import context._
  implicit val timeout = Timeout(5.seconds)

  private val updatesPusher = context.actorOf(Props(new PusherActor(handleActor)))
  private val fupdatesBroker = UpdatesBroker.lookup(uid, publicKeyHash)
  private var subscribedToUpdates = false
  private val differenceSize = 500

  protected def handleRequestGetDifference(p: Package, messageId: Long)(
    seq: Int, state: BitVector
  ) = {
    subscribeToUpdates(uid, publicKeyHash)

    uuidCodec.decodeValue(state) match {
      case \/-(uuid) =>
        for {
          updatesBroker <- fupdatesBroker
          seq <- ask(updatesBroker, UpdatesBroker.GetSeq).mapTo[Int]
          difference <- CommonUpdateRecord.getDifference(
            uid, publicKeyHash, uuid, differenceSize + 1
          ) flatMap (mkDifference(seq, _))
        } yield {
          handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(difference))).right)
        }
      case -\/(e) =>
        // TODO: handle properly
        throw new Exception("Wrong state format")
    }
  }

  protected def handleRequestGetState(p: Package, messageId: Long) = {
    subscribeToUpdates(uid, publicKeyHash)

    val f = getState(uid, publicKeyHash)

    f onComplete {
      case Success((seq, muuid)) =>
        val rsp = updateRpcProto.State(seq, muuid map (uuidCodec.encodeValid) getOrElse BitVector.empty)
        handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
      case Failure(err) =>
        // TODO: Handle failure
        throw new Exception("Failed to get state")
    }
  }

  protected def mkDifference(seq: Int, allUpdates: immutable.Seq[Entity[UUID, updateProto.CommonUpdateMessage]]): Future[Difference] = {
    val needMore = allUpdates.length > differenceSize
    val updates = allUpdates.tail
    val users = mkUsers(authId, updates)
    val state = uuidCodec.encodeValid(updates.last.key)
    users map (Difference(seq, state, _,
      updates map {u => DifferenceUpdate(u.value)}, needMore))
  }

  protected def mkUsers(authId: Long, updates: immutable.Seq[Entity[UUID, updateProto.CommonUpdateMessage]]): Future[immutable.Vector[struct.User]] = {
    @inline
    def getUserStruct(uid: Int): Future[Option[struct.User]] = {
      UserRecord.getEntity(uid) map (_ map (_.toStruct(authId)))
    }

    val userIds = updates map (_.value.userIds) reduceLeft ((x, y) => x ++ y)
    Future.sequence(userIds.map(getUserStruct(_)).toVector) map (_.flatten)
  }

  protected def subscribeToUpdates(uid: Int, publicKeyHash: Long) = {
    if (!subscribedToUpdates) {
      log.info("Subscribing to updates uid={} publicKeyHash={}", uid, publicKeyHash)
      mediator ! Subscribe(UpdatesBroker.topicFor(uid, publicKeyHash), updatesPusher)
    }
  }

  @inline
  protected def getState(uid: Int, publicKeyHash: Long)(implicit session: CSession): Future[(Int, Option[UUID])] = {
    for {
      seq <- getSeq()
      muuid <- CommonUpdateRecord.getState(uid, publicKeyHash)
    } yield {
      (seq, muuid)
    }
  }

  private def getSeq(): Future[Int] = {
    fupdatesBroker flatMap (ask(_, UpdatesBroker.GetSeq).mapTo[Int])
  }
}

sealed class PusherActor(handleActor: ActorRef) extends Actor with ActorLogging {
  lazy val rand = new Random

  def receive = {
    case (u: updateProto.Message, seq: Int, state: UUID) =>
      val upd = CommonUpdate(seq, uuidCodec.encode(state).toOption.get, u)
      val ub = UpdateBox(upd)
      handleActor ! UpdateBoxToSend(ub)
  }
}
