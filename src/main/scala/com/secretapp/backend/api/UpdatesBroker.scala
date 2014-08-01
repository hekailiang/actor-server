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

  def topicFor(authId: Long): String = s"updates-${authId.toString}"

  def lookup(authId: Long)
    (implicit system: ActorSystem, session: CSession, timeout: Timeout): Future[ActorRef] = {
    val path = s"broker-${UpdatesBroker.topicFor(authId)}"
    SharedActors.lookup(path) {
      system.actorOf(Props(new UpdatesBroker(authId)), path)
    }
  }
}

class UpdatesBroker(val authId: Long)(implicit session: CSession) extends PersistentActor with ActorLogging {
  import context.dispatcher

  override def persistenceId = s"updates-broker-${authId.toString}"

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(authId)

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
            CommonUpdateRecord.push(authId, u)(session) map { uuid =>
              mediator ! Publish(topic, u)
              replyTo ! (seq, uuid)
              log.info(
                s"Pushed update authId=$authId seq=${this.seq}, state=$uuid update=$update"
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
