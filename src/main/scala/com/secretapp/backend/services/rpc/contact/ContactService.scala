package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist.PhoneRecord
import com.secretapp.backend.data.models.User
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable.Seq
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
        handleRequestImportContacts(contacts)
      }
  }

  def handleRequestImportContacts(contacts: Seq[ContactToImport]): Future[RpcResponse] = {
    val clientPhoneMap = contacts.map(c => c.phoneNumber -> c.clientPhoneId).toMap
    val authId = currentAuthId
    for {
      phones <- PhoneRecord.getEntities(contacts.map(_.phoneNumber))
    } yield {

      val (users, contacts, uids) = phones.foldLeft((Seq[struct.User](), Seq[ImportedContact](), Set[Int]())) {
        case ((users, contacts, uids), p) =>
          val u = struct.User(
            p.userId,
            User.getAccessHash(
              authId,
              p.userId,
              p.userAccessSalt),
            p.userName,
            p.userSex.toOption,
            p.userKeyHashes,
            p.number)
          val c = ImportedContact(clientPhoneMap(p.number), p.userId)

          (u +: users, c +: contacts, uids + p.userId)
      }

      socialBrokerRegion ! SocialMessageBox(currentUser.get.uid, RelationsNoted(uids))
      Ok(ResponseImportedContacts(users, contacts))
    }
  }
}
