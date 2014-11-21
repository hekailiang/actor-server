package com.secretapp.backend.session

import akka.actor._
import scala.annotation.tailrec
import scala.collection.immutable
import scodec.bits.BitVector

object AckTrackerProtocol {
  sealed trait AckTrackerMessage

  case class RegisterMessage(key: Long, value: BitVector) extends AckTrackerMessage
  case class RegisterMessageAck(key: Long) extends AckTrackerMessage
  case class RegisterMessageAcks(keys: List[Long]) extends AckTrackerMessage

  // perhaps in future we will need it to be case class with key or key and value
  case object MessageAlreadyRegistered extends AckTrackerMessage
  case object GetUnackdMessages extends AckTrackerMessage
  case class MessagesSizeOverflow(key: Long) extends AckTrackerMessage
  case class UnackdMessages(messages: immutable.Map[Long, BitVector]) extends AckTrackerMessage
}

/**
 * Actor for tracking undelivered things
 *
 * @param sizeLimit things size limit - when it's reached actor sends dies to prevent too large memory consumption
 */
class AckTrackerActor(authId: Long, sessionId: Long, sizeLimit: Int) extends Actor with ActorLogging {
  import AckTrackerProtocol._
  import context._

  case class State(messages: immutable.Map[Long, BitVector], messagesSize: Int) {
    /**
     * Add new element
     */
    def withNew(key: Long, message: BitVector): State = {
      State(messages + Tuple2(key, message), messagesSize + (message.size / 8).toInt)
    }

    /**
     * Remove element (consider delivered)
     */
    def without(key: Long): State = {
      messages.get(key) match {
        case Some(message) =>
          State(messages - key, messagesSize - (message.size / 8).toInt)
        case None =>
          //log.warning("Trying to remove element which is not present in undelivered things list")
          this
      }
    }
  }

  var state = State(immutable.Map[Long, BitVector](), 0)

  def receive = {
    case m: RegisterMessage =>
      //log.debug(s"RegisterMessage ${m.key} size=${state.messagesSize}")
      val newState = state.withNew(m.key, m.value)

      if (newState.messagesSize > sizeLimit) {
        log.warning("Messages size overflow")
        sender() ! MessagesSizeOverflow(m.key)
        context stop self
      } else {
        state = newState
      }
    case m: RegisterMessageAck =>
      registerMessageAck(m.key)
    case ms: RegisterMessageAcks =>
      //log.debug(s"RegisterMessageAcks $ms")
      ms.keys.foreach(registerMessageAck)
    case GetUnackdMessages =>
      //log.debug(s"GetUnackdMessages $state")
      sender() ! UnackdMessages(state.messages)
  }

  /**
   * Register message ack
   */
  private def registerMessageAck(key: Long) = {
    state = state.without(key)
  }
}
