package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist.PhoneRecord
import com.secretapp.backend.data.models.User
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
        handleRequestImportContacts(contacts)
      }
  }

  def handleRequestImportContacts(contacts: immutable.Seq[ContactToImport]): Future[RpcResponse] = {
    val clientPhoneMap = contacts.map(c => c.phoneNumber -> c.clientPhoneId).toMap
    val authId = currentAuthId
    for {
      items <- PhoneRecord.getEntities(contacts.map(_.phoneNumber))
    } yield {
      // FIXME: Fuck this three cycles in a row!
      val users = items.map { item =>
        struct.User(uid = item.userId, accessHash = User.getAccessHash(authId, item.userId, item.userAccessSalt),
          phoneNumber = item.number, keyHashes = item.userKeyHashes, name = item.userName,
          sex = item.userSex.toOption)
      }
      val contacts = items.map { user =>
        ImportedContact(clientPhoneId = clientPhoneMap(user.number), userId = user.userId)
      }

      val uids: Set[Int] = items.map(_.userId).toSet
      socialBrokerRegion ! SocialMessageBox(currentUser.get.uid, RelationsNoted(uids))

      Ok(ResponseImportedContacts(users, contacts))
    }
  }
}
