package com.secretapp.backend.helpers

import akka.actor._
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.collection.immutable
import scala.concurrent.Future

trait ContactHelpers extends UpdatesHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.{ dispatcher, system }

  protected def addContact(
    ownerUserId: Int,
    userId: Int,
    phoneNumber: Long,
    name: String,
    accessSalt: String
  ): Future[Unit] = {
    val newContactsId = Set(userId)
    val clFuture = persist.contact.UserContactsList.insertContact(ownerUserId, userId, phoneNumber, name, accessSalt)
    val clCacheFuture = persist.contact.UserContactsListCache.addContactsId(ownerUserId, newContactsId)

    for {
      _ <- clFuture
      _ <- clCacheFuture
    } yield ()
  }

  protected def addContactSendUpdate(
    ownerUser: models.User,
    userId: Int,
    phoneNumber: Long,
    name: String,
    accessSalt: String
  )(implicit timeout: Timeout): Future[UpdatesBroker.StrictState] = {
    addContact(ownerUser.uid, userId, phoneNumber, name, accessSalt) flatMap { _ =>
      broadcastCUUpdateAndGetState(
        ownerUser,
        updateProto.contact.ContactsAdded(Vector(userId))
      )
    }
  }
}
