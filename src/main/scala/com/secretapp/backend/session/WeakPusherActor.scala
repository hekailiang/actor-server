package com.secretapp.backend.session

import akka.actor._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.{ struct, update => updateProto, RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update.WeakUpdate
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.rpc.presence._

class WeakPusherActor(sessionActor: ActorRef, authId: Long) extends Actor with ActorLogging {
  import PresenceProtocol._

  def receive = {
    case u: UserOnlineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
    case u: UserOfflineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
    case u: UserLastSeenUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
  }
}
