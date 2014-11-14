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

  def withNewUpdateState[A](authId: Long, update: SeqUpdateMessage)
    (f: UpdatesBroker.StrictState => A)
    (implicit timeout: Timeout) = {
    ask(
      updatesBrokerRegion,
      UpdatesBroker.NewUpdatePush(
        authId,
        update
      )).mapTo[UpdatesBroker.StrictState] map f
  }
}
