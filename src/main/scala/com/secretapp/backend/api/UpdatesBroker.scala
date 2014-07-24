package com.secretapp.backend.api

import akka.actor._
import akka.persistence._
import akka.util.Timeout
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.CommonUpdateRecord
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import scala.concurrent.Future

object UpdatesBroker {
  case class NewUpdate[A <: updateProto.CommonUpdateMessage](update: A)
  case object GetSeq

  def topicFor(userId: Long, publicKeyHash: Long): String = s"updates-${userId.toString}-${publicKeyHash.toString}"
  def topicFor(user: User): String = topicFor(user.uid, user.publicKeyHash)

  def lookup(userId: Int, publicKeyHash: Long)
    (implicit system: ActorSystem, session: CSession, timeout: Timeout): Future[ActorRef] = {
    val path = s"broker~${UpdatesBroker.topicFor(userId, publicKeyHash)}"
    SharedActors.lookup(path) {
      system.actorOf(Props(new UpdatesBroker(userId, publicKeyHash)), path)
    }
  }
  def lookup(user: User)
    (implicit system: ActorSystem, session: CSession, timeout: Timeout): Future[ActorRef] =
    lookup(user.uid, user.publicKeyHash)
}

class UpdatesBroker(val userId: Int, val publicKeyHash: Long)(implicit session: CSession) extends PersistentActor with ActorLogging {
  import context.dispatcher

  override def persistenceId = s"updates-broker-${userId}-${publicKeyHash.toString()}"

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(userId, publicKeyHash)

  var seq: Int = 0

  val receiveCommand: Receive = {
    case UpdatesBroker.GetSeq =>
      sender ! this.seq
    case p @ UpdatesBroker.NewUpdate(update) =>
      log.info(s"NewUpdate {}", update)
      val replyTo = sender
      persist(p) { _ =>
        update match {
          case u: updateProto.CommonUpdateMessage =>
            seq += 1
            // FIXME: Handle errors!
            CommonUpdateRecord.push(userId, publicKeyHash, u)(session) map { uuid =>
              mediator ! Publish(topic, u)
              replyTo ! (seq, uuid)
              log.info(
                s"Pushed update uid=$userId publicKeyHash=$publicKeyHash seq=${this.seq}, state=$uuid update=$update"
              )
            }
        }
      }
  }

  def receiveRecover: Actor.Receive = {
    case RecoveryCompleted =>
    case s =>
      println(s"Recovering ${s}")
      this.seq += 1
  }
}
