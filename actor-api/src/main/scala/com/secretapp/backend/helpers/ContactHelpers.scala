package com.secretapp.backend.helpers

import akka.actor._
import akka.util.Timeout
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.message.{update => updateProto, struct}
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.persist.contact.UserContact
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future, blocking}

trait ContactHelpers extends UpdatesHelpers {
  val context: ActorContext

  import context.{ dispatcher, system }

  protected def addContact(
    ownerUserId: Int,
    userId: Int,
    phoneNumber: Long,
    name: Option[String],
    accessSalt: String
  ): Future[Unit] = {
    val newContactsId = Set(userId)
    val clFuture = persist.contact.UserContact.createOrRestore(ownerUserId, userId, phoneNumber, name, accessSalt)

    for {
      _ <- clFuture
    } yield ()
  }

  protected def addContactSendUpdate(
    ownerUser: models.User,
    userId: Int,
    phoneNumber: Long,
    name: Option[String],
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

object ContactHelpers {
  def createAllUserContacts(ownerUserId: Int, contacts: immutable.Seq[(struct.User, String)])
                           (implicit ec: ExecutionContext): Future[List[models.contact.UserContact]] = Future {
    blocking {
      /* use batch if upsert is available (in mysql for example)
      val batchParams: Seq[Seq[Any]] = contacts map {
        case (userStruct, accessSalt) =>
          Seq(
            ownerUserId,
            contact.uid, // contactUserId
            contact.phoneNumber, // phoneNumber
            contact.localName.getOrElse(""), // name
            accessSalt
          )
      }
      sql"""
      insert into ${UserContact.table} (
        ${column.ownerUserId},
        ${column.contactUserId},
        ${column.phoneNumber},
        ${column.name},
        ${column.accessSalt}
       ) VALUES (
        ?, ?, ?, ?, ?
       )
      """.batch(batchParams: _*).apply
       */

      UserContact.findAllExistingContactIdsSync(ownerUserId, contacts.map(_._1.uid).toSet)
    }
  } flatMap { existingContactUserIds =>
    val futures = contacts.toList map {
      case (userStruct, accessSalt) =>
        val userContact = models.contact.UserContact(
          ownerUserId = ownerUserId,
          contactUserId = userStruct.uid,
          phoneNumber = userStruct.phoneNumber,
          name = userStruct.localName,
          accessSalt = accessSalt
        )

        if (existingContactUserIds.contains(userStruct.uid)) {
          UserContact.save(userContact).map(_ => userContact)
        } else {
          UserContact.createOrRestore(
            ownerUserId = userContact.ownerUserId,
            contactUserId = userContact.contactUserId,
            phoneNumber = userContact.phoneNumber,
            name = userContact.name,
            accessSalt = userContact.accessSalt
          ) map (_ => userContact)
        }
    }

    Future.sequence(futures)
  }
}
