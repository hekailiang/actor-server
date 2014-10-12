package com.secretapp.backend.session

import akka.actor._
import akka.persistence._
import scala.annotation.tailrec
import scala.collection.immutable
import akka.util.ByteString

object AckTrackerProtocol {
  sealed trait AckTrackerMessage

  case class RegisterMessage(key: Long, value: ByteString) extends AckTrackerMessage
  case class RegisterMessageAck(key: Long) extends AckTrackerMessage
  case class RegisterMessageAcks(keys: List[Long]) extends AckTrackerMessage

  // perhaps in future we will need it to be case class with key or key and value
  case object MessageAlreadyRegistered extends AckTrackerMessage
  case object GetUnackdMessages extends AckTrackerMessage
  case class MessagesSizeOverflow(key: Long) extends AckTrackerMessage
  case class UnackdMessages(messages: immutable.Map[Long, ByteString]) extends AckTrackerMessage
}

/**
 * Actor for tracking undelivered things
 *
 * @param sizeLimit things size limit - when it's reached actor sends dies to prevent too large memory consumption
 */
class AckTrackerActor(authId: Long, sessionId: Long, sizeLimit: Int) extends PersistentActor with ActorLogging {
  import AckTrackerProtocol._
  import context._

  override def persistenceId: String = s"ackTracker-$authId-$sessionId"

  case class State(messages: immutable.Map[Long, ByteString], messagesSize: Int) {
    /**
     * Add new element
     */
    def withNew(key: Long, message: ByteString): State = {
      State(messages + Tuple2(key, message), messagesSize + message.size)
    }

    /**
     * Remove element (consider delivered)
     */
    def without(key: Long): State = {
      messages.get(key) match {
        case Some(message) =>
          State(messages - key, messagesSize - message.size)
        case None =>
          log.warning("Trying to remove element which is not present in undelivered things list")
          this
      }
    }
  }

  var state = State(immutable.Map[Long, ByteString](), 0)

  def receiveCommand: Receive = {
    case m: RegisterMessage =>
      log.debug(s"RegisterMessage $persistenceId ${m.key} size=${state.messagesSize}")
      persist(m) { _ =>
        val newState = state.withNew(m.key, m.value)

        if (newState.messagesSize > sizeLimit) {
          log.warning("Messages size overflow")
          sender() ! MessagesSizeOverflow(m.key)
          context stop self
        } else {
          state = newState
        }
      }
    case m: RegisterMessageAck =>
      persist(m){ _ =>
        registerMessageAck(m.key)
      }
    case ms: RegisterMessageAcks =>
      log.debug(s"RegisterMessageAcks $ms")
      persist(ms)(_.keys.foreach(registerMessageAck))
    case GetUnackdMessages =>
      log.debug(s"GetUnackdMessages $state")
      sender() ! UnackdMessages(state.messages)
  }

  def receiveRecover: Receive = {
    case m: RegisterMessage =>
      log.debug(s"recovering $persistenceId $m")
      state = state.withNew(m.key, m.value)
    case m: RegisterMessageAck =>
      registerMessageAck(m.key)
    case ms: RegisterMessageAcks =>
      ms.keys.foreach(registerMessageAck)
  }

  /**
   * Register message ack
   */
  private def registerMessageAck(key: Long) = {
    state = state.without(key)
  }
}
