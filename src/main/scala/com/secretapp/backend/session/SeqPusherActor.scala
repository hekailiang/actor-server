package com.secretapp.backend.session

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.{ struct, update => updateProto, UpdateBox }
import com.secretapp.backend.data.message.update.{ FatSeqUpdate, SeqUpdate }
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scala.concurrent.Future
import scodec.codecs.{ uuid => uuidCodec }

private[session] class SeqPusherActor(sessionActor: ActorRef, authId: Long)
                                     (implicit val session: CSession) extends Actor with ActorLogging {
  import context.dispatcher
  import context.system

  def receive = {
    case (seq: Int, state: UUID, u: updateProto.SeqUpdateMessage) =>
      log.info(s"Pushing update to session seq=$seq authId=$authId $u")
      val fupd = u match {
        case _: ContactRegistered | _: ContactsAdded =>
          val fuserStructs = u.userIds map { userId =>
            persist.User.getEntity(userId) map (_ map (struct.User.fromModel(_, authId)))
          }
          for {
            userStructs <- Future.sequence(fuserStructs)
          } yield {
            FatSeqUpdate(seq, uuidCodec.encode(state).toOption.get, u, userStructs.flatten.toVector)
          }
        case _ =>
          Future.successful(SeqUpdate(seq, uuidCodec.encode(state).toOption.get, u))
      }

      fupd map { upd =>
        val ub = UpdateBox(upd)
        sessionActor ! UpdateBoxToSend(ub)
      }
    case u =>
      log.error(s"Unknown update in topic $u")
  }
}
