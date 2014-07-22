package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.util.Timeout
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
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
import scala.concurrent.duration._
import scodec.codecs.uuid
import scodec.bits._
import scalaz._
import akka.pattern.ask
import Scalaz._

class UpdatesManager(val handleActor: ActorRef, val uid: Int, val publicKeyHash: Long)(implicit val session: CSession) extends Actor with ActorLogging with UpdatesService {
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(uid, publicKeyHash)

  def receive = {
    case RpcProtocol.Request(p, messageId, updateRpcProto.RequestGetDifference(seq, state)) =>
      val stateUuid = uuid.decodeValidValue(state)
      CommonUpdateRecord.getDifference(uid, publicKeyHash, stateUuid)
  }
}

sealed trait UpdatesService {
  self: UpdatesManager =>

  import context._
  implicit val timeout = Timeout(5.seconds)

  private val updatesPusher = context.actorOf(Props(new PusherActor(handleActor)))
  private val fupdatesBroker = UpdatesBroker.lookup(uid, publicKeyHash)
  private var subscribedToUpdates = false

  protected def handleRequestGetDifference(p: Package, messageId: Long, currentUser: User)(
    seq: Int, state: BitVector
  ) = {

  }

  protected def handleRequestGetState(p: Package, messageId: Long, currentUser: User) = {
    subscribeToUpdates(currentUser)

    val f = getState(currentUser)

    f onComplete {
      case Success((seq, muuid)) =>
        val rsp = updateRpcProto.State(seq, muuid map (uuid.encodeValid) getOrElse BitVector.empty)
        handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
      case Failure(err) =>
        // TODO: Handle failure
        throw new Exception("Failed to get state")
    }
  }

  protected def subscribeToUpdates(currentUser: User) = {
    if (!subscribedToUpdates) {
      log.info("Subscribing to updates {}", currentUser)
      mediator ! Subscribe(UpdatesBroker.topicFor(currentUser), updatesPusher)
    }
  }

  protected def getState(currentUser: User)(implicit session: CSession): Future[(Int, Option[UUID])] = {
    for {
      seq <- getSeq()
      muuid <- CommonUpdateRecord.getState(currentUser.uid, currentUser.publicKeyHash)
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
      val upd = CommonUpdate(seq, uuid.encode(state).toOption.get, u)
      //val mb = MessageBox(rand.nextLong, upd)
      //handleActor ! MessageBoxToSend(mb)
  }
}
