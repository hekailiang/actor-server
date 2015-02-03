package com.secretapp.backend.session

import akka.actor._
import com.secretapp.backend.data.message.{ struct, update => updateProto, UpdateBox }
import com.secretapp.backend.data.message.update.{ FatSeqUpdate, SeqUpdate }
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.helpers.{ AuthIdOwnershipHelpers, UserHelpers }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import im.actor.util.logging.AlertingActor
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import scodec.codecs.{ uuid => uuidCodec }

private[session] class SeqPusherActor(sessionActor: ActorRef, authId: Long)
    extends AlertingActor
    with ActorLogging
    with UserHelpers
    with AuthIdOwnershipHelpers {
  import context.dispatcher
  import context.system

  def receive = {
    case msg @ (seq: Int, state: UUID, u: updateProto.SeqUpdateMessage) =>
      val fupd = u match {
        case _: ContactRegistered | _: ContactsAdded =>
          getOrSetUserId(authId) flatMap { userId =>
            val fuserStructs = u.userIds map { structUserId =>
              getUserStruct(structUserId, authId, userId)
            }
            for { userStructs <- Future.sequence(fuserStructs) }
            yield FatSeqUpdate(seq, uuidCodec.encode(state).toOption.get, u, userStructs.flatten.toVector, Vector.empty)
          }
        case _ =>
          Future.successful(SeqUpdate(seq, uuidCodec.encodeValid(state), u))
      }

      fupd onComplete {
        case Success(upd) =>
          val ub = UpdateBox(upd)
          sessionActor ! UpdateBoxToSend(ub)
        case Failure(e) =>
          log.error(e, "Failed to build update {}", msg)
      }
    case u =>
      log.error(s"Unknown update in topic $u")
  }
}
