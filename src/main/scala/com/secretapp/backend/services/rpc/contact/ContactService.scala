package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{UpdatesBroker, SocialProtocol, ApiBrokerService}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist.{UnregisteredContactRecord, PhoneRecord, UserRecord}
import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scodec.codecs.uuid

trait ContactService {
  self: ApiBrokerService with GeneratorService with UserManagerService =>

  implicit val session: CSession

  import context._
  import SocialProtocol._

  def handleRpcContact: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case RequestImportContacts(phones, emails) =>
      authorizedRequest {
        handleRequestImportContacts(phones, emails)
      }
  }

  def handleRequestImportContacts(phones: immutable.Seq[PhoneToImport],
                                  emails: immutable.Seq[EmailToImport]): Future[RpcResponse] = {
    val currentUser = getUser.get
    val filteredPhones = phones.filter(_.phoneNumber != currentUser.phoneNumber) // TODO: remove user.get
    val phoneNumbers = filteredPhones.map(_.phoneNumber).toSet
    val authId = currentAuthId
    val usersSeq = for {
      phones <- PhoneRecord.getEntities(phoneNumbers)
//      newPhones <- UserContactsListCacheRecord.getPhoneNumbers()
      // TODO: log warning on None result from UserRecord.getEntity
      usersFutureSeq <- Future.sequence(phones map (p => UserRecord.getEntity(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      usersFutureSeq.foldLeft(immutable.Seq[struct.User](), immutable.Set[Int](), immutable.Set[Long]()) {
        case ((users, uids, registeredPhones), user) =>
          (users :+ struct.User.fromModel(user, authId), uids + user.uid, registeredPhones + user.phoneNumber)
      }
    }

    usersSeq flatMap {
      case (users, uids, registeredPhones) =>
        uids foreach { socialBrokerRegion ! SocialMessageBox(_, RelationsNoted(Set(currentUser.uid))) } // TODO: wrap as array!

        (phoneNumbers &~ registeredPhones).foreach { phoneNumber => // TODO: move into singleton method
          UnregisteredContactRecord.insertEntity(models.UnregisteredContact(phoneNumber, currentUser.uid))
        }

        val stateFuture = ask(
          updatesBrokerRegion,
          UpdatesBroker.NewUpdatePush(currentUser.authId, updateProto.contact.UpdateContactsAdded(uids.toIndexedSeq))
        ).mapTo[UpdatesBroker.StrictState]

        stateFuture map {
          case (seq, state) => Ok(ResponseImportedContacts(users, seq, uuid.encodeValid(state)))
        }
    }
  }
}
