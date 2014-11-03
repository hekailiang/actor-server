package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.rpc.RpcErrors
import com.secretapp.backend.api.{UpdatesBroker, SocialProtocol, ApiBrokerService}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.persist.contact._
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist.{UnregisteredContactRecord, PhoneRecord, UserRecord}
import com.secretapp.backend.models
import com.secretapp.backend.api.rpc.RpcValidators._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.util.ACL
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
    case RequestDeleteContact(contactId, accessHash) =>
      authorizedRequest {
        handleRequestDeleteContact(contactId, accessHash)
      }
    case RequestEditContactName(contactId, accessHash, localName) =>
      authorizedRequest {
        handleRequestEditContactName(contactId, accessHash, localName)
      }
  }

  def handleRequestImportContacts(phones: immutable.Seq[PhoneToImport],
                                  emails: immutable.Seq[EmailToImport]): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    val filteredPhones = phones.filter(_.phoneNumber != currentUser.phoneNumber) // TODO: remove user.get
    val phoneNumbers = filteredPhones.map(_.phoneNumber).toSet
    val phonesMap = immutable.HashMap(filteredPhones.map { p => p.phoneNumber -> p.contactName } :_*)
    val usersSeq = for {
      phones <- PhoneRecord.getEntities(phoneNumbers)
      ignoredContactsId <- UserContactsListCacheRecord.getContactsAndDeletedId(currentUser.uid)
      uniquePhones = phones.filter(p => !ignoredContactsId.contains(p.userId))
      usersFutureSeq <- Future.sequence(uniquePhones map (p => UserRecord.getEntity(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      usersFutureSeq.foldLeft(immutable.Seq[(struct.User, String)](), immutable.Set[Int](), immutable.Set[Long]()) {
        case ((usersTuple, newContactsId, registeredPhones), user) =>
          (usersTuple :+ (struct.User.fromModel(user, authId, phonesMap(user.phoneNumber)), user.accessSalt),
            newContactsId + user.uid,
            registeredPhones + user.phoneNumber)
      }
    }

    usersSeq flatMap {
      case (usersTuple, newContactsId, registeredPhones) =>
        if (usersTuple.nonEmpty) {
          newContactsId foreach {
            socialBrokerRegion ! SocialMessageBox(_, RelationsNoted(Set(currentUser.uid))) // TODO: wrap as array!
          }

          (phoneNumbers &~ registeredPhones).foreach { phoneNumber => // TODO: move into singleton method
            UnregisteredContactRecord.insertEntity(models.UnregisteredContact(phoneNumber, currentUser.uid))
          }

          val clFuture = UserContactsListRecord.insertNewContacts(currentUser.uid, usersTuple)
          val clCacheFuture = UserContactsListCacheRecord.addContactsId(currentUser.uid, newContactsId)
          val stateFuture = ask(
            updatesBrokerRegion,
            UpdatesBroker.NewUpdatePush(currentUser.authId,
              updateProto.contact.ContactsAdded(newContactsId.toIndexedSeq))
          ).mapTo[UpdatesBroker.StrictState].map {
            case (seq, state) => Ok(ResponseImportedContacts(usersTuple.map(_._1), seq, uuid.encodeValid(state)))
          }

          for {
            _ <- clFuture
            _ <- clCacheFuture
            response <- stateFuture
          } yield response
        } else Future.successful(Ok(ResponseImportedContacts(immutable.Seq[struct.User](), 0, BitVector.empty)))
    }
  }

  def handleRequestGetContacts(contactsHash: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    UserContactsListCacheRecord.getContactsId(currentUser.uid) flatMap { contactsId =>
      if (contactsHash == UserContactsListCacheRecord.getSHA1Hash(contactsId)) {
        Future.successful(Ok(ResponseGetContacts(immutable.Seq[struct.User](), isNotChanged = true)))
      } else {
        for {
          contactList <- UserContactsListRecord.getEntitiesWithLocalName(currentUser.uid)
          usersFutureSeq <- Future.sequence(contactList.map { c => UserRecord.getEntity(c._1) }).map(_.flatten) // TODO: OPTIMIZE!!!
        } yield {
          val localNames = immutable.HashMap(contactList :_*)
          val users = usersFutureSeq.map { user =>
            struct.User.fromModel(user, authId, localNames.get(user.uid))
          }
          Ok(ResponseGetContacts(users.toIndexedSeq, isNotChanged = false))
        }
      }
    }
  }

  def handleRequestDeleteContact(contactId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    UserContactsListRecord.getContact(currentUser.uid, contactId) flatMap {
      case Some(contact) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, contact.accessSalt)) {
          val clFuture = UserContactsListRecord.removeContact(currentUser.uid, contactId)
          val stateFuture = ask(
            updatesBrokerRegion,
            UpdatesBroker.NewUpdatePush(currentUser.authId, updateProto.contact.ContactsRemoved(immutable.Seq(contactId)))
          ).mapTo[UpdatesBroker.StrictState].map {
            case (seq, state) => Ok(ResponseSeq(seq, state.some))
          }

          for {
            _ <- clFuture
            response <- stateFuture
          } yield response
        } else Future.successful(RpcErrors.invalidAccessHash)
      case _ => Future.successful(RpcErrors.entityNotFound("CONTACT"))
    }
  }

  def handleRequestEditContactName(contactId: Int, accessHash: Long, name: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    UserRecord.getAccessSaltAndPhone(contactId) flatMap {
      case Some((accessSalt, phoneNumber)) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, accessSalt)) {
          withValidName(name) { name =>
            val clFuture = UserContactsListRecord.insertContact(currentUser.uid, contactId, phoneNumber, name, accessSalt)
            val stateFuture = ask(
              updatesBrokerRegion,
              UpdatesBroker.NewUpdatePush(currentUser.authId, updateProto.contact.LocalNameChanged(contactId, name.some))
            ).mapTo[UpdatesBroker.StrictState].map {
              case (seq, state) => Ok(ResponseSeq(seq, state.some))
            }

            for {
              _ <- clFuture
              response <- stateFuture
            } yield response
          }
        } else Future.successful(RpcErrors.invalidAccessHash)
      case _ => Future.successful(RpcErrors.entityNotFound("CONTACT"))
    }
  }
}
