package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{UpdatesBroker, SocialProtocol, ApiBrokerService}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.persist.contact._
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist.{UnregisteredContactRecord, PhoneRecord, UserRecord}
import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scodec.bits.BitVector
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
    case RequestGetContacts(contactsHash) =>
      authorizedRequest {
        handleRequestGetContacts(contactsHash)
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
      existsContactsId <- UserContactsListCacheRecord.getContactsId(currentUser.uid)
      uniquePhones = phones.filter(p => !existsContactsId.contains(p.userId))
      usersFutureSeq <- Future.sequence(uniquePhones map (p => UserRecord.getEntity(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      val t = usersFutureSeq.foldLeft(immutable.Seq[struct.User](), immutable.Set[Int](), immutable.Set[Long]()) {
        case ((users, uids, registeredPhones), user) =>
          (users :+ struct.User.fromModel(user, authId), uids + user.uid, registeredPhones + user.phoneNumber)
      }
      (t, existsContactsId)
    }

    usersSeq flatMap {
      case ((users, uids, registeredPhones), existsContactsId) =>
        if (users.nonEmpty) {
          uids foreach {
            socialBrokerRegion ! SocialMessageBox(_, RelationsNoted(Set(currentUser.uid))) // TODO: wrap as array!
          }

          (phoneNumbers &~ registeredPhones).foreach { phoneNumber => // TODO: move into singleton method
            UnregisteredContactRecord.insertEntity(models.UnregisteredContact(phoneNumber, currentUser.uid))
          }

          val phonesMap = immutable.HashMap(filteredPhones.map { p => p.phoneNumber -> p.contactName } :_*)
          val contacts = users.map { u =>
            phonesMap(u.phoneNumber) match {
              case Some(name) if !name.nonEmpty => u.copy(name = name)
              case _ => u
            }
          }
          UserContactsListRecord.insertEntities(currentUser.uid, contacts)
          UserContactsListCacheRecord.upsertEntity(currentUser.uid, existsContactsId ++ uids)

          val stateFuture = ask(
            updatesBrokerRegion,
            UpdatesBroker.NewUpdatePush(currentUser.authId, updateProto.contact.UpdateContactsAdded(uids.toIndexedSeq))
          ).mapTo[UpdatesBroker.StrictState]

          stateFuture map {
            case (seq, state) => Ok(ResponseImportedContacts(contacts, seq, uuid.encodeValid(state)))
          }
        } else Future.successful(Ok(ResponseImportedContacts(users, 0, BitVector.empty)))
    }
  }

  def handleRequestGetContacts(contactsHash: String): Future[RpcResponse] = {
    Future.successful(Ok(ResponseGetContacts(immutable.Seq[struct.User](), isNotChanged = false)))
  }
}
