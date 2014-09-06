package com.secretapp.backend.protocol.codecs.message.update

import scala.util.{ Try, Success, Failure }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

object WeakUpdateMessageCodec {
  def encode(body: WeakUpdateMessage): String \/ BitVector = {
    body match {
      case m: UserOnlineUpdate => UserOnlineUpdateCodec.encode(m)
      case m: UserOfflineUpdate => UserOfflineUpdateCodec.encode(m)
      case m: UserLastSeenUpdate => UserLastSeenUpdateCodec.encode(m)
    }
  }

  def decode(weakUpdateType: Int, buf: BitVector): String \/ WeakUpdateMessage = {
    val tried = Try(weakUpdateType match {
      case UserOnlineUpdate.weakUpdateType => UserOnlineUpdateCodec.decode(buf)
      case UserOfflineUpdate.weakUpdateType => UserOfflineUpdateCodec.decode(buf)
      case UserLastSeenUpdate.weakUpdateType => UserLastSeenUpdateCodec.decode(buf)
    })
    tried match {
      case Success(res) => res match {
        case \/-(r) => r._2.right
        case l@(-\/(_)) => l
      }
      case Failure(e) => e.getMessage.left
    }
  }
}
