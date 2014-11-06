package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs.ByteConstants
import java.util.UUID
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._

object OptionalUUIDCodec extends Codec[Option[UUID]] {
  import ByteConstants._

  def encode(optState: Option[UUID]) = optState match {
    case None => BitVector.empty.right
    case Some(uuidState) =>
      uuid.encode(uuidState) match {
        case \/-(bytesState) =>
          bytesState.right
        case l => l
      }
  }

  def decode(buf: BitVector) = {
    buf match {
      case BitVector.empty => (BitVector.empty, None).right
      case bv =>
        uuid.decodeValue(bv) match {
          case \/-(uuidState) => (BitVector.empty, Some(uuidState)).right
          case l @ -\/(_) => l
        }
    }
  }
}
