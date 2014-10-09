package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist.{UnregisteredContactRecord, PhoneRecord, UserRecord}
import com.secretapp.backend.data.models.{UnregisteredContact, User}
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait ContactService {
  self: ApiBrokerService with GeneratorService with UserManagerService =>

  implicit val session: CSession

  import context._
  import SocialProtocol._

  def handleRpcContact: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case RequestImportContacts(contacts) =>
      authorizedRequest {
        handleRequestImportContacts(contacts.toSet)
      }
  }

  def handleRequestImportContacts(rawContacts: Set[ContactToImport]): Future[RpcResponse] = {
    val contacts = rawContacts.filter(_.phoneNumber != currentUser.get.phoneNumber)
    val clientPhoneMap = contacts.map(c => c.phoneNumber -> c.clientPhoneId).toMap
    val authId = currentAuthId
    val res = for {
      phones <- PhoneRecord.getEntities(contacts.map(_.phoneNumber))
      // TODO: log warning on None result from UserRecord.getEntity
      users <- Future.sequence(phones map (p => UserRecord.getEntity(p.userId))) map (_.flatten)
    } yield {
      users.foldLeft(
        (immutable.Seq[struct.User](), immutable.Seq[ImportedContact](), immutable.Set[Int](), immutable.Set[Long]())) {
        case ((userStructs, impContacts, uids, registeredPhones), user) =>
          val u = struct.User(
            user.uid,
            User.getAccessHash(authId, user.uid, user.accessSalt),
            user.name,
            user.sex.toOption,
            user.keyHashes,
            user.phoneNumber,
            user.avatar
          )
          val c = ImportedContact(clientPhoneMap(user.phoneNumber), user.uid)

          (u +: userStructs, c +: impContacts, uids + user.uid, registeredPhones + user.phoneNumber)
      }
    }

    res flatMap {
      case (userStructs, impContacts, uids, registeredPhones) => {
        uids foreach { uid =>
          socialBrokerRegion ! SocialMessageBox(uid, RelationsNoted(Set(currentUser.get.uid)))
        }

        val unregisteredPhones = contacts.map(_.phoneNumber) &~ registeredPhones
        val unregisteredContacts = unregisteredPhones.map(UnregisteredContact(_, currentUser.get.authId))

        Future.sequence(unregisteredContacts.map(UnregisteredContactRecord.insertEntity)) map { _ =>
          Ok(ResponseImportedContacts(userStructs, impContacts))
        }
      }
    }

  }
}
