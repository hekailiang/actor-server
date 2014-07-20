package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.persist._
import scala.concurrent.Future

object UpdatesProtocol {
  case class NewUpdate[A <: updateProto.UpdateMessage](update: A)
  case object GetState
}

object UpdatesManager {
  case class State(seq: Int) {
    def incremented = copy(seq = seq + 1)
  }

  def topicFor(keyHash: Long) = s"updates-${keyHash.toString()}"
}

class UpdatesManager(keyHash: Long)(implicit session: CSession) extends PersistentActor with ActorLogging {
  override def persistenceId = s"updates-manager-${keyHash.toString()}"

  import context.dispatcher

  var state = UpdatesManager.State(0)

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesManager.topicFor(keyHash)

  def receiveCommand: Actor.Receive = {
    case UpdatesProtocol.GetState =>
      val replyTo = sender
      CommonUpdateRecord.getState(keyHash)(session) map { muuid =>
        replyTo ! (state.seq, muuid)
      }
    case p @ UpdatesProtocol.NewUpdate(update) =>
      log.info(s"NewUpdate {}", update)
      val replyTo = sender
      persist(p) { _ =>
        update match {
          case u: updateProto.UpdateMessage =>
            state = state.incremented
            // FIXME: Handle errors!
            CommonUpdateRecord.push(keyHash, state.seq, u)(session) map { uuid =>
              log.info("Pushed update seq={}, state={}", state.seq, uuid)
              mediator ! Publish(topic, u)
              replyTo ! (state, uuid)
            }
        }
      }
  }

  def receiveRecover: Actor.Receive = {
    case RecoveryCompleted =>
    case s =>
      println(s"Recovering ${s}")
      state = state.incremented
  }
}
