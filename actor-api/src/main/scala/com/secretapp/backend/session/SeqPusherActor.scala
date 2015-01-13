package com.secretapp.backend.session

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.{ struct, update => updateProto, UpdateBox }
import com.secretapp.backend.data.message.update.{ FatSeqUpdate, SeqUpdate }
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scala.concurrent.Future
import scodec.codecs.{ uuid => uuidCodec }

private[session] class SeqPusherActor(sessionActor: ActorRef, authId: Long)
                                     (implicit val session: CSession) extends Actor with ActorLogging with UserHelpers {
  import context.dispatcher
  import context.system

  def receive = {
    case (seq: Int, state: UUID, u: updateProto.SeqUpdateMessage) =>
      val fupd = u match {
        case _: ContactRegistered | _: ContactsAdded =>
          val fuserStructs = u.userIds map { userId =>
            getUserStruct(userId, authId)
          }
          for { userStructs <- Future.sequence(fuserStructs) }
          yield FatSeqUpdate(seq, uuidCodec.encode(state).toOption.get, u, userStructs.flatten.toVector, Vector.empty)
        case _ =>
          Future.successful(SeqUpdate(seq, uuidCodec.encodeValid(state), u))
      }

      fupd map { upd =>
        val ub = UpdateBox(upd)
        sessionActor ! UpdateBoxToSend(ub)
      }
    case u =>
      log.error(s"Unknown update in topic $u")
  }
}
