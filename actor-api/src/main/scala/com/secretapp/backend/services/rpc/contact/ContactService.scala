package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.rpc.RpcErrors
import com.secretapp.backend.api.{ UpdatesBroker, SocialProtocol, ApiBrokerService, PhoneNumber }
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.helpers.UpdatesHelpers
import com.secretapp.backend.services.{ UserManagerService, GeneratorService }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist
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

trait ContactService extends UpdatesHelpers {
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
    case RequestRemoveContact(contactId, accessHash) =>
      authorizedRequest {
        handleRequestRemoveContact(contactId, accessHash)
      }
    case RequestEditUserLocalName(contactId, accessHash, localName) =>
      authorizedRequest {
        handleRequestEditUserLocalName(contactId, accessHash, localName)
      }
    case RequestSearchContacts(request) =>
      authorizedRequest {
        handleRequestSearchContacts(request)
      }
    case RequestAddContact(userId, accessHash) =>
      authorizedRequest {
        handleRequestAddContact(userId, accessHash)
      }
  }

  def handleRequestImportContacts(phones: immutable.Seq[PhoneToImport],
                                  emails: immutable.Seq[EmailToImport]): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get // TODO: remove user.get
    val filteredPhones = phones.filter(_.phoneNumber != currentUser.phoneNumber)
    val phoneNumbers = filteredPhones.map(_.phoneNumber).map(PhoneNumber.normalizeLong(_, currentUser.countryCode)).flatten.toSet
    val phonesMap = immutable.HashMap(filteredPhones.map { p => p.phoneNumber -> p.contactName } :_*)
    val usersSeq = for {
      phones <- persist.Phone.getEntities(phoneNumbers)
      ignoredContactsId <- persist.contact.UserContactsListCache.getContactsAndDeletedId(currentUser.uid)
      uniquePhones = phones.filter(p => !ignoredContactsId.contains(p.userId))
      usersFutureSeq <- Future.sequence(uniquePhones map (p => persist.User.getEntity(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
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
            persist.UnregisteredContact.insertEntity(models.UnregisteredContact(phoneNumber, currentUser.uid))
          }

          val clFuture = persist.contact.UserContactsList.insertNewContacts(currentUser.uid, usersTuple)
          val clCacheFuture = persist.contact.UserContactsListCache.addContactsId(currentUser.uid, newContactsId)
          val responseFuture = broadcastCUUpdateAndGetState(
            currentUser,
            updateProto.contact.ContactsAdded(newContactsId.toIndexedSeq)
          ) map {
            case (seq, state) => Ok(ResponseImportedContacts(usersTuple.map(_._1), seq, state.some))
          }

          for {
            _ <- clFuture
            _ <- clCacheFuture
            response <- responseFuture
          } yield response
        } else Future.successful(Ok(ResponseImportedContacts(immutable.Seq[struct.User](), 0, None)))
    }
  }

  def handleRequestGetContacts(contactsHash: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    persist.contact.UserContactsListCache.getContactsId(currentUser.uid) flatMap { contactsId =>
      if (contactsHash == persist.contact.UserContactsListCache.getSHA1Hash(contactsId)) {
        Future.successful(Ok(ResponseGetContacts(immutable.Seq[struct.User](), isNotChanged = true)))
      } else {
        for {
          contactList <- persist.contact.UserContactsList.getEntitiesWithLocalName(currentUser.uid)
          usersFutureSeq <- Future.sequence(contactList.map { c => persist.User.getEntity(c._1) }).map(_.flatten) // TODO: OPTIMIZE!!!
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

  def handleRequestRemoveContact(contactId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    persist.contact.UserContactsList.getContact(currentUser.uid, contactId) flatMap {
      case Some(contact) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, contact.accessSalt)) {
          val clFuture = persist.contact.UserContactsList.removeContact(currentUser.uid, contactId)

          getAuthIds(currentUser.uid) flatMap { authIds =>
            authIds foreach { authId =>
              if (authId != currentUser.authId) {
                writeNewUpdate(authId, updateProto.contact.LocalNameChanged(contactId, None))
                writeNewUpdate(authId, updateProto.contact.ContactsRemoved(immutable.Seq(contactId)))
              }
            }

            writeNewUpdate(currentUser.authId, updateProto.contact.LocalNameChanged(contactId, None))

            withNewUpdateState(
              currentUser.authId,
              updateProto.contact.ContactsRemoved(immutable.Seq(contactId))
            ) {
              case (seq, state) => Ok(ResponseSeq(seq, state.some))
            }
          }
        } else Future.successful(RpcErrors.invalidAccessHash)
      case _ => Future.successful(RpcErrors.entityNotFound("CONTACT"))
    }
  }

  def handleRequestEditUserLocalName(contactId: Int, accessHash: Long, name: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    persist.User.getEntity(contactId) flatMap {
      case Some(user) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, user.accessSalt)) {
          val clFuture = persist.contact.UserContactsList.getContact(currentUser.uid, contactId) flatMap {
            case Some(contact) =>
              persist.contact.UserContactsList.insertContact(currentUser.uid, user.uid, user.phoneNumber, name, user.accessSalt)
            case None =>
              addContact(user.uid, user.phoneNumber, name, user.accessSalt, currentUser)
          }

          clFuture flatMap { _ =>
            broadcastCUUpdateAndGetState(
              currentUser,
              updateProto.contact.LocalNameChanged(contactId, name.some)
            ) map {
              case (seq, state) => Ok(ResponseSeq(seq, state.some))
            }
          }
        } else Future.successful(RpcErrors.invalidAccessHash)
      case _ => Future.successful(RpcErrors.entityNotFound("CONTACT"))
    }
  }

  def handleRequestSearchContacts(request: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    PhoneNumber.normalizeStr(request, currentUser.countryCode) match {
      case None => Future.successful(Ok(ResponseSearchContacts(immutable.Seq[struct.User]())))
      case Some(phoneNumber) =>
        val filteredPhones = Set(phoneNumber).filter(_ != currentUser.phoneNumber)
        for {
          phones <- persist.Phone.getEntities(filteredPhones)
          users <- Future.sequence(phones map (p => persist.User.getEntity(p.userId))).map(_.flatten)
        } yield Ok(ResponseSearchContacts(users.map(struct.User.fromModel(_, authId)).toIndexedSeq))
    }
  }

  def handleRequestAddContact(userId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    if (userId == currentUser.uid) {
      Future.successful(RpcErrors.cantAddSelf)
    } else {
      persist.User.getEntity(userId) flatMap {
        case Some(user) =>
          if (accessHash == ACL.userAccessHash(authId, userId, user.accessSalt)) {
            addContact(user.uid, user.phoneNumber, "", user.accessSalt, currentUser) map {
              case (seq, state) => Ok(ResponseSeq(seq, state.some))
            }
          } else Future.successful(RpcErrors.invalidAccessHash)
        case None => Future.successful(RpcErrors.entityNotFound("USER"))
      }
    }
  }

  private def addContact(userId: Int, phoneNumber: Long, name: String, accessSalt: String, currentUser: models.User): Future[UpdatesBroker.StrictState] = {
    val newContactsId = Set(userId)
    val clFuture = persist.contact.UserContactsList.insertContact(currentUser.uid, userId, phoneNumber, name, accessSalt)
    val clCacheFuture = persist.contact.UserContactsListCache.addContactsId(currentUser.uid, newContactsId)
    val stateFuture = broadcastCUUpdateAndGetState(
      currentUser,
      updateProto.contact.ContactsAdded(newContactsId.toIndexedSeq)
    )

    for {
      _ <- clFuture
      _ <- clCacheFuture
      state <- stateFuture
    } yield state
  }
}
