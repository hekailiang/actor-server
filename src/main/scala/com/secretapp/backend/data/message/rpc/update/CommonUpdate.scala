package com.secretapp.backend.data.message.rpc.update

import scala.language.implicitConversions
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import com.getsecretapp.{ proto => protobuf }
import scodec.bits.BitVector

case class CommonUpdate(seq : Int, state : BitVector, body : UpdateMessage) extends RpcResponseMessage
{
  def toProto = {
    // TODO: kill me if I don't refactor this within two days since 15.07.14 (SA-19)
    val (updateType, update : BitVector) = body match {
      case m : Message => (Message.updateType, MessageCodec.encode(m).toOption.get)
      case m : MessageSent => (MessageSent.updateType, MessageSentCodec.encode(m).toOption.get)
      case n : NewDevice => (NewDevice.updateType, NewDeviceCodec.encode(n).toOption.get)
      case n : NewYourDevice => (NewYourDevice.updateType, NewYourDeviceCodec.encode(n).toOption.get)
      case _ => throw new Throwable("refactor this codec!#SA-19")
    }
    protobuf.CommonUpdate(seq, state, updateType, update)
  }
}

object CommonUpdate extends RpcResponseMessageObject {
  val responseType = 0xd

  def fromProto(u : protobuf.CommonUpdate) : CommonUpdate = u match {
    case protobuf.CommonUpdate(seq, state, updateId, body) =>
      val update : UpdateMessage = updateId match {
        case Message.updateType => MessageCodec.decode(body).toOption.get._2
        case MessageSent.updateType => MessageSentCodec.decode(body).toOption.get._2
        case NewDevice.updateType => NewDeviceCodec.decode(body).toOption.get._2
        case NewYourDevice.updateType => NewYourDeviceCodec.decode(body).toOption.get._2
        case _ => throw new Throwable("refactor this codec!#SA-19")
      }
      CommonUpdate(seq, state, update)
  }
}
