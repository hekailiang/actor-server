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
      case m: UserOnline => UserOnlineCodec.encode(m)
      case m: UserOffline => UserOfflineCodec.encode(m)
      case m: UserLastSeen => UserLastSeenCodec.encode(m)
    }
  }

  def decode(weakUpdateHeader: Int, buf: BitVector): String \/ WeakUpdateMessage = {
    val tried = Try(weakUpdateHeader match {
      case UserOnline.weakUpdateHeader => UserOnlineCodec.decode(buf)
      case UserOffline.weakUpdateHeader => UserOfflineCodec.decode(buf)
      case UserLastSeen.weakUpdateHeader => UserLastSeenCodec.decode(buf)
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
