package com.secretapp.backend.protocol.codecs.message.rpc

import com.google.protobuf.ByteString
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success

object ErrorCodec extends Codec[Error] with utils.ProtobufCodec {
  val codec = (int32 ~ protoString ~ protoString ~ protoBool ~ protoBytes): Codec[Int ~ String ~ String ~ Boolean ~ BitVector]

  def encode(e: Error) = {
    val eEncodedData = e.data match {
      case Some(data: WrongReceiversErrorData) =>
        encodeToBitVector(data.toProto)
      case Some(w) =>
        s"Wrong error data $w".left
      case None => BitVector.empty.right
    }

    eEncodedData match {
      case \/-(data) =>
        codec.encode(e.code ~ e.tag ~ e.userMessage ~ e.canTryAgain ~ data)
      case e @ -\/(_) =>
        e
    }
  }

  def decode(buf: BitVector) = {
    codec.decode(buf) match {
      case \/-((rem, code ~ tag ~ userMessage ~ canTryAgain ~ encData)) =>
        val eitherData = encData match {
          case BitVector.empty =>
            None.right
          case _ =>
            decodeProtobuf(protobuf.WrongReceiversErrorData.parseFrom(encData.toByteArray)) {
              case Success(protoData: protobuf.WrongReceiversErrorData) =>
                Some(WrongReceiversErrorData.fromProto(protoData))
            } match {
              case \/-((_, r)) => r.right
              case e @ -\/(_) => e
            }
        }

        eitherData match {
          case \/-(data) =>
            (rem, Error(code, tag, userMessage, canTryAgain, data)).right
          case e @ -\/(_) => e
        }

      case e @ -\/(_) => e
    }
  }
}
