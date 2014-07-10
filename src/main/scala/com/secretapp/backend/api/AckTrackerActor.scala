package com.secretapp.backend.api

import akka.actor._
import scala.annotation.tailrec
import scala.collection.immutable
import akka.util.ByteString

sealed trait AckTrackerMessage

case class RegisterMessage[K](key: K, value: ByteString) extends AckTrackerMessage
case class RegisterMessageAck[K](key: K) extends AckTrackerMessage

// perhaps in future we will need it to be case class with key or key and value
case object MessageAlreadyRegistered extends AckTrackerMessage
case object GetUnackdMessages extends AckTrackerMessage
case object MessagesSizeOverflow extends AckTrackerMessage
case class UnackdMessages[K](messages: immutable.Map[K, ByteString]) extends AckTrackerMessage

/**
  * Actor for tracking undelivered things
  *
  * @param sizeLimit things size limit - when it's reached actor sends dies to prevent too large memory consumption
  */
class AckTrackerActor[K](val sizeLimit: Int) extends Actor with ActorLogging {
  import context._

  case class State(messages: immutable.Map[K, ByteString], messagesSize: Int)

  def trackMessages(state: State): Receive = {
    case m: RegisterMessage[K] =>
      state.messages.get(m.key) match {
        case Some(value) =>
          sender ! MessageAlreadyRegistered
        case None =>
          val newState = addNew(state, m.key, m.value)

          if (newState.messagesSize > sizeLimit) {
            log.warning("Messages size overflow")
            sender ! MessagesSizeOverflow
            context stop self
          } else {
            become(trackMessages(newState))
          }
      }
    case m: RegisterMessageAck[K] =>
      val newState = remove(state, m.key)
      become(trackMessages(newState))
    case GetUnackdMessages =>
      sender ! UnackdMessages(state.messages)
  }

  def receive = trackMessages(State(immutable.Map[K, ByteString](), 0))

  /**
    * Add new element
    */
  private def addNew(state: State, key: K, message: ByteString): State = {
    State(state.messages + Tuple2(key, message), state.messagesSize + message.size)
  }

  /**
    * Remove element (consider delivered)
    */
  private def remove(state: State, key: K): State = {
    state.messages.get(key) match {
      case Some(message) =>
        State(state.messages - key, state.messagesSize - message.size)
      case None =>
        log.info("Trying to remove element which is not present in undelivered things list")
        state
    }
  }
}
