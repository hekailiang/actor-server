package com.secretapp.backend.api

import akka.actor._
import akka.persistence._
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.persist._
import scala.concurrent.Future

object UpdatesProtocol {
  case class NewUpdate[A <: updateProto.UpdateMessage](update: A)
}

object UpdatesManager {
  case class State(seq: Int) {
    def incremented = copy(seq = seq + 1)
  }
}
/*
class GlobalUpdatesManager(session: CSession) extends Actor with ActorLogging  {
  import UpdatesProtocol._
  import context._

  implicit val _session = session

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe("global-updates", self)

  val sequences = mutable.HashMap[Long, Long]()

  def receive = {
    case NewUpdate(userId, update) =>
      userSessions(userId) map { sessions =>
        sessions foreach { sessionId =>
          //CommonUpdateRecord.push(userId, sessionId, update)
        }
      }
  }

  def userSessions(userId: Long): Future[Set[Long]] = Future {
    Set(userId)
  }
}
 */
class UpdatesManager(keyHash: Long)(implicit val session: CSession) extends PersistentActor with ActorLogging {
  import context._
  import UpdatesManager._

  override def persistenceId = s"updates-manager-${keyHash}"

  var state = UpdatesManager.State(0)

  def receiveCommand: Actor.Receive = {
    case p @ UpdatesProtocol.NewUpdate(update) =>
      val replyTo = sender
      persist(p) { _ =>
        update match {
          case u: updateProto.UpdateMessage =>
            state = state.incremented
            // FIXME: Handle errors!
            CommonUpdateRecord.push(keyHash, state.seq, u) map { uuid =>
              replyTo ! (state, uuid)
            }
          case u: updateProto.MessageSent =>
            state = state.incremented
            CommonUpdateRecord.push(keyHash, state.seq, u) map { uuid =>
              replyTo ! (state, uuid)
            }
        }
      }
  }

  def receiveRecover: Actor.Receive = {
    case _ => state = state.incremented
  }
}
