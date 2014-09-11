package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.persistence._
import scala.collection.immutable

object SocialProtocol {
  sealed trait SocialMessage
  type PersistentStateType = immutable.Set[Int]
  type RelationsType = PersistentStateType

  case class RelationsNoted(uids: Set[Int]) extends SocialMessage
  case object GetRelations extends SocialMessage

  case class SocialMessageBox(userId: Int, event: SocialMessage)
}

object SocialBroker {
  import SocialProtocol._

  private val shardCount = 2 // TODO: configurable

  private val idExtractor: ShardRegion.IdExtractor = {
    case msg @ SocialMessageBox(userId, _) => (userId.toString, msg)
  }

  private val shardResolver: ShardRegion.ShardResolver = {
    case msg@SocialMessageBox(userId, _) => (userId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName = "SocialBroker",
    entryProps = Some(Props(new SocialBroker)),
    idExtractor = idExtractor,
    shardResolver = shardResolver)
}

class SocialBroker extends PersistentActor with ActorLogging {
  import SocialProtocol._

  override def persistenceId: String = self.path.parent.name + self.path.name

  val minSnapshotStep = 10
  var lastSnapshottedAtSize = 0
  var uids: PersistentStateType = immutable.Set.empty

  val receiveCommand: Actor.Receive = {
    case msg @ SocialMessageBox(userId, RelationsNoted(newUids)) if newUids.size > 0 =>
      log.info(s"SocialMessageBox $userId $newUids")
      persist(msg) { _ =>
        uids = uids ++ newUids
        maybeSnapshot()
      }
    case SocialMessageBox(userId, GetRelations) =>
      log.info(s"GetRelations $userId")
      sender ! uids
  }

  val receiveRecover: Actor.Receive = {
    case SnapshotOffer(metadata, offeredSnapshot) =>
      log.debug(s"SnapshotOffer $metadata $offeredSnapshot")
      uids = offeredSnapshot.asInstanceOf[PersistentStateType]
    case msg @ SocialMessageBox(_, RelationsNoted(newUids)) if newUids.size > 0 =>
      log.debug(s"Recovering $msg")
      uids = uids ++ newUids
    case RecoveryCompleted =>
      log.debug("Recovery completed")
  }

  private def maybeSnapshot(): Unit = {
    if ((uids.size - lastSnapshottedAtSize) > minSnapshotStep) {
      log.debug("Saving snapshot")
      lastSnapshottedAtSize = uids.size
      saveSnapshot(uids)
    }
  }
}
