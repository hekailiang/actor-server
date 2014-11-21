package com.secretapp.backend.helpers

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.rpc.{ Error, RpcResponse }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future

trait UpdatesHelpers {
  val context: ActorContext

  import context.dispatcher

  val updatesBrokerRegion: ActorRef

  def writeNewUpdate(authId: Long, update: SeqUpdateMessage): Unit = {
    updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(
      authId, update
    )
  }

  def writeNewUpdateAndGetState(authId: Long, update: SeqUpdateMessage)
    (implicit timeout: Timeout): Future[UpdatesBroker.StrictState] = {
    ask(
      updatesBrokerRegion,
      UpdatesBroker.NewUpdatePush(
        authId,
        update
      )).mapTo[UpdatesBroker.StrictState]
  }

  def withNewUpdateState[A](authId: Long, update: SeqUpdateMessage)
    (f: UpdatesBroker.StrictState => A)
    (implicit timeout: Timeout): Future[A] = {
    writeNewUpdateAndGetState(authId, update) map f
  }

  def withNewUpdatesState[A](authId: Long, updates: Seq[SeqUpdateMessage])
    (f: UpdatesBroker.StrictState => A)
    (implicit timeout: Timeout): Future[A] = {
    if (updates.length > 0) {
      updates.init foreach { update =>
        updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(
          authId,
          update
        )
      }
      ask(
        updatesBrokerRegion,
        UpdatesBroker.NewUpdatePush(
          authId,
          updates.last
        )
      ).mapTo[UpdatesBroker.StrictState] map f
    } else {
      throw new Exception("Zero updates size")
    }
  }
}
