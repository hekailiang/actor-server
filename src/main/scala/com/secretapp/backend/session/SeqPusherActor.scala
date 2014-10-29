package com.secretapp.backend.session

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.{ struct, update => updateProto, UpdateBox }
import com.secretapp.backend.data.message.update.{ ContactRegistered, FatSeqUpdate, SeqUpdate }
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.common.PackageCommon._
import java.util.UUID
import scala.concurrent.Future
import scodec.codecs.{ uuid => uuidCodec }

private[session] class SeqPusherActor(sessionActor: ActorRef, authId: Long)
                                     (implicit val session: CSession) extends Actor with ActorLogging {
  import context.dispatcher

  def receive = {
    case (seq: Int, state: UUID, u: updateProto.SeqUpdateMessage) =>
      log.info(s"Pushing update to session seq=$seq authId=$authId $u")
      val fupd = u match {
        case _: ContactRegistered =>
          val fuserStructs = u.userIds map { userId =>
            UserRecord.getEntity(userId) map (_ map (struct.User.fromModel(_, authId)(context.system)))
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
