package com.secretapp.backend.session

import akka.actor.ActorRef
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.models
import com.secretapp.backend.data.transport.MessageBox
import im.actor.server.protobuf.{ ProtobufMessage, ProtobufMessageObject }
import scala.collection.immutable

object SessionProtocol {
  import im.actor.server.protobuf.protocol.{ Session => PBSession }

  // TODO: wrap all messages into Envelope
  sealed trait SessionMessage

  sealed trait HandleMessageBox {
    val mb: MessageBox
  }
  @SerialVersionUID(1L)
  case class HandleMTMessageBox(mb: MessageBox) extends HandleMessageBox with SessionMessage

  @SerialVersionUID(1L)
  case class AuthorizeUser(user: models.User) extends SessionMessage

  @SerialVersionUID(1L)
  case class AuthorizeUserNew(user: models.UserNew) extends SessionMessage with ProtobufMessage[PBSession.AuthorizeUser] {
    def asMessage =
      PBSession.AuthorizeUser.newBuilder.setUser(user.asMessage).build
  }

  object AuthorizeUserNew extends ProtobufMessageObject[PBSession.AuthorizeUser, AuthorizeUserNew] {
    val parseMessageFrom = PBSession.AuthorizeUser.parseFrom: Array[Byte] => PBSession.AuthorizeUser

    def fromMessage(m: PBSession.AuthorizeUser) = AuthorizeUserNew(models.UserNew.fromMessage(m.getUser))
  }

  @SerialVersionUID(1L)
  case class SendRpcResponseBox(connector: ActorRef, rpcBox: RpcResponseBox) extends SessionMessage

  @SerialVersionUID(1L)
  case object SubscribeToUpdates extends SessionMessage

  @SerialVersionUID(1L)
  case class SubscribeToPresences(uids: immutable.Seq[Int]) extends SessionMessage

  // TODO: remove in preference to UnsubscribeFromPresences
  @SerialVersionUID(1L)
  case class UnsubscribeToPresences(uids: immutable.Seq[Int]) extends SessionMessage

  @SerialVersionUID(1L)
  case class UnsubscribeFromPresences(uids: immutable.Seq[Int]) extends SessionMessage

  @SerialVersionUID(1L)
  case class SubscribeToGroupPresences(groupIds: immutable.Seq[Int]) extends SessionMessage
  @SerialVersionUID(1L)
  case class UnsubscribeFromGroupPresences(groupIds: immutable.Seq[Int]) extends SessionMessage

  case class Envelope(authId: Long, sessionId: Long, payload: SessionMessage)
}
