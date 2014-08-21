package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.api.SocialProtocol
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types
import com.secretapp.backend.services.RpcService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.persist.PhoneRecord
import com.secretapp.backend.data.models.User
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable
import scalaz._
import Scalaz._

trait ContactService extends PackageCommon with RpcCommon {
  self: RpcService with Actor with GeneratorService with UserManagerService =>

  implicit val session: CSession

  import context._
  import SocialProtocol._

  def handleRpcContact(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case RequestImportContacts(contacts) =>
      sendRpcResult(p, messageId)(handleRequestImportContacts(p)(contacts))
  }

  def handleRequestImportContacts(p: Package)(contacts: immutable.Seq[ContactToImport]): RpcResult = {
    val clientPhoneMap = contacts.map(c => c.phoneNumber -> c.clientPhoneId).toMap
    val authId = p.authId // TODO
    for {
      items <- PhoneRecord.getEntities(contacts.map(_.phoneNumber))
    } yield {
      val authId = p.authId
      // FIXME: Fuck this three cycles in a row!
      val users = items.map { item =>
        struct.User(uid = item.userId, accessHash = User.getAccessHash(authId, item.userId, item.userAccessSalt),
          phoneNumber = item.number, keyHashes = item.userKeyHashes, firstName = item.userFirstName,
          lastName = item.userLastName, sex = item.userSex.toOption)
      }
      val contacts = items.map { user =>
        ImportedContact(clientPhoneId = clientPhoneMap(user.number), userId = user.userId)
      }

      val uids: Set[Int] = items.map(_.userId).toSet
      socialBrokerRegion ! SocialMessageBox(currentUser.get.uid, RelationsNoted(uids))

      ResponseImportedContacts(users, contacts).right
    }
  }
}
