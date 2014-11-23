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
    case msg@SocialMessageBox(userId, _) => (userId % shardCount).abs.toString
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
      persist(msg) { _ =>
        uids = uids ++ newUids.filterNot(_ == userId)
        maybeSnapshot()
      }
    case SocialMessageBox(userId, GetRelations) =>
      sender ! uids
  }

  val receiveRecover: Actor.Receive = {
    case SnapshotOffer(metadata, offeredSnapshot) =>
      uids = offeredSnapshot.asInstanceOf[PersistentStateType]
    case msg @ SocialMessageBox(userId, RelationsNoted(newUids)) if newUids.size > 0 =>
      uids = uids ++ newUids.filterNot(_ == userId)
  }

  private def maybeSnapshot(): Unit = {
    if ((uids.size - lastSnapshottedAtSize) > minSnapshotStep) {
      lastSnapshottedAtSize = uids.size
      saveSnapshot(uids)
    }
  }
}
