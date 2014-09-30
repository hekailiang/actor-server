package com.secretapp.backend.session

import akka.actor._
import com.secretapp.backend.data.message.{ struct, update => updateProto, RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update.SeqUpdate
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scodec.codecs.{ uuid => uuidCodec }

private[session] class SeqPusherActor(sessionActor: ActorRef, authId: Long) extends Actor with ActorLogging {
  def receive = {
    case (seq: Int, state: UUID, u: updateProto.SeqUpdateMessage) =>
      log.info(s"Pushing update to session authId=$authId $u")
      val upd = SeqUpdate(seq, uuidCodec.encode(state).toOption.get, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
    case u =>
      log.error(s"Unknown update in topic $u")
  }
}
