package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.api.rpc.RpcErrors
import com.secretapp.backend.api.{ UpdatesBroker, SocialProtocol, ApiBrokerService, PhoneNumber }
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.helpers.{ ContactHelpers, UpdatesHelpers, UserHelpers }
import com.secretapp.backend.services.{ UserManagerService, GeneratorService }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.persist
import com.secretapp.backend.models
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait ContactService extends UpdatesHelpers with ContactHelpers with UserHelpers {
  self: ApiBrokerService with GeneratorService with UserManagerService =>

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

    val f = for {
      userPhones <- persist.UserPhone.findAllByNumbers(phoneNumbers)
      ignoredContactsId <- persist.contact.UserContact.findAllContactIdsAndDeleted(currentUser.uid)
      uniquePhones = userPhones.filter(p => !ignoredContactsId.contains(p.userId))
      usersDatasWithAvatars <- Future.sequence(uniquePhones map (p => persist.User.findDataWithAvatar(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      val userPhoneNumbers = userPhones.map(_.number).toSet

      usersDatasWithAvatars.foldLeft((immutable.Seq.empty[(struct.User, String)], immutable.Set.empty[Int], userPhoneNumbers)) {
        case ((usersTuple, newContactsId, _), (userData, avatarData)) =>
          (
            usersTuple :+ (struct.User.fromData(userData, avatarData, authId, phonesMap(userData.phoneNumber)), userData.accessSalt),
            newContactsId + userData.id,
            userPhoneNumbers
          )
      }
    }

    f flatMap {
      case (usersTuple, newContactsId, registeredPhoneNumbers) =>
        log.debug("Phone numbers: {}, registered: {}", phoneNumbers, registeredPhoneNumbers)

        (phoneNumbers &~ registeredPhoneNumbers).foreach { phoneNumber => // TODO: move into singleton method
          log.debug("Inserting UnregisteredContact {} {}", phoneNumber, currentUser.uid)
          persist.UnregisteredContact.createIfNotExists(phoneNumber, currentUser.uid)
        }

        if (usersTuple.nonEmpty) {
          newContactsId foreach {
            socialBrokerRegion ! SocialMessageBox(_, RelationsNoted(Set(currentUser.uid))) // TODO: wrap as array!
          }

          val clFuture = ContactHelpers.createAllUserContacts(currentUser.uid, usersTuple)
          val responseFuture = broadcastCUUpdateAndGetState(
            currentUser,
            updateProto.contact.ContactsAdded(newContactsId.toIndexedSeq)
          ) map {
            case (seq, state) => Ok(ResponseImportContacts(usersTuple.map(_._1), seq, state.some))
          }

          for {
            _ <- clFuture
            response <- responseFuture
          } yield response
        } else Future.successful(Ok(ResponseImportContacts(immutable.Seq[struct.User](), 0, None)))
    }
  }

  def handleRequestGetContacts(contactsHash: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    persist.contact.UserContact.findAllContactIds(currentUser.uid) flatMap { contactsId =>
      if (contactsHash == persist.contact.UserContact.getSHA1Hash(contactsId)) {
        Future.successful(Ok(ResponseGetContacts(immutable.Seq[struct.User](), isNotChanged = true)))
      } else {
        for {
          contactList <- persist.contact.UserContact.findAllContactIdsWithLocalNames(currentUser.uid)
          usersDatasFutureSeq <- Future.sequence(contactList.map { c => persist.User.findDataWithAvatar(c._1) }).map(_.flatten) // TODO: OPTIMIZE!!!
        } yield {
          val localNames = immutable.HashMap(contactList :_*)
          val users = usersDatasFutureSeq.map {
            case (userData, avatarData) =>
              struct.User.fromData(userData, avatarData, authId, localNames.get(userData.id).flatten)
          }
          Ok(ResponseGetContacts(users.toIndexedSeq, isNotChanged = false))
        }
      }
    }
  }

  def handleRequestRemoveContact(contactId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    persist.contact.UserContact.find(ownerUserId = currentUser.uid, contactUserId = contactId) flatMap {
      case Some(contact) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, contact.accessSalt)) {
          val clFuture = persist.contact.UserContact.destroy(currentUser.uid, contactId)

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
    persist.User.findData(contactId) flatMap {
      case Some(userData) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, userData.accessSalt)) {
          val clFuture = persist.contact.UserContact.find(ownerUserId = currentUser.uid, contactUserId = contactId) flatMap {
            case Some(contact) =>
              persist.contact.UserContact.save(models.contact.UserContact(
                currentUser.uid, userData.id, userData.phoneNumber, Some(name), userData.accessSalt
              ))
            case None =>
              addContactSendUpdate(currentUser, userData.id, userData.phoneNumber, Some(name), userData.accessSalt)
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
      case None => Future.successful(Ok(ResponseSearchContacts(immutable.Seq.empty)))
      case Some(phoneNumber) =>
        val filteredPhones = Set(phoneNumber).filter(_ != currentUser.phoneNumber)
        for {
          userPhones <- persist.UserPhone.findAllByNumbers(filteredPhones)
          usersDatasAvatars <- Future.sequence(userPhones map (p => persist.User.findDataWithAvatar(p.userId))).map(_.flatten)
        } yield {
          userPhones foreach (up => socialBrokerRegion ! SocialMessageBox(up.userId, RelationsNoted(Set(currentUser.uid))))

          Ok(ResponseSearchContacts(usersDatasAvatars.map(ua => struct.User.fromData(ua._1, ua._2, authId, None)).toIndexedSeq))
        }
    }
  }

  def handleRequestAddContact(userId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    if (userId == currentUser.uid) {
      Future.successful(RpcErrors.cantAddSelf)
    } else {
      persist.User.findData(userId) flatMap {
        case Some(userData) =>
          if (accessHash == ACL.userAccessHash(authId, userId, userData.accessSalt)) {
            persist.contact.UserContact.find(ownerUserId = currentUser.uid, contactUserId = userId) flatMap {
              case None =>
                addContactSendUpdate(currentUser, userData.id, userData.phoneNumber, None, userData.accessSalt) map {
                  case (seq, state) => Ok(ResponseSeq(seq, state.some))
                }
              case Some(_) =>
                Future.successful(RpcErrors.entityAlreadyExists("CONTACT"))
            }
          } else Future.successful(RpcErrors.invalidAccessHash)
        case None => Future.successful(RpcErrors.entityNotFound("USER"))
      }
    }
  }
}
