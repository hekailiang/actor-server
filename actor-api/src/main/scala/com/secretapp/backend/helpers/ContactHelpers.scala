package com.secretapp.backend.helpers

import akka.actor._
import akka.util.Timeout
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.{update => updateProto, struct}
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.collection.immutable
import scala.concurrent.Future

trait ContactHelpers extends UpdatesHelpers {
  val context: ActorContext

  import context.{ dispatcher, system }

  protected def addContact(
    ownerUserId: Int,
    userId: Int,
    phoneNumber: Long,
    name: String,
    accessSalt: String
  ): Future[Unit] = {
    val newContactsId = Set(userId)
    val clFuture = persist.contact.UserContact.create(ownerUserId, userId, phoneNumber, name, accessSalt)

    for {
      _ <- clFuture
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

//  protected def insertContact(currentUser: models.User, newContactsId: Set[Int], usersTuple: immutable.Seq[(struct.User, String)])
//                             (implicit timeout: Timeout) = for {
//    _ <- persist.contact.UserContactsList.insertNewContacts(currentUser.uid, usersTuple)
//    _ <- persist.contact.UserContactsListCache.addContactsId(currentUser.uid, newContactsId)
//    state <- broadcastCUUpdateAndGetState(
//      currentUser,
//      updateProto.contact.ContactsAdded(newContactsId.toIndexedSeq)
//    )
//  } yield state
}
