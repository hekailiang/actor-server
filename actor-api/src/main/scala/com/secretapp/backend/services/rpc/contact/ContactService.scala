package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
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
import com.secretapp.backend.api.rpc.RpcValidators._
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{ Success, Failure }
import scalaz._
import Scalaz._
import scodec.bits.BitVector
import scodec.codecs.uuid

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
      usersWithAvatars <- Future.sequence(uniquePhones map (p => persist.User.findWithAvatar(p.userId)(None))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      val userPhoneNumbers = userPhones map (_.number) toSet

      usersWithAvatars.foldLeft((immutable.Seq.empty[(struct.User, String)], immutable.Set.empty[Int], userPhoneNumbers)) {
        case ((usersTuple, newContactsId, _), (user, avatarData)) =>
          (
            usersTuple :+ (struct.User.fromModel(user, avatarData, authId, phonesMap(user.phoneNumber)), user.accessSalt),
            newContactsId + user.uid,
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

          val clFuture = persist.contact.UserContact.createAll(currentUser.uid, usersTuple)
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
        } else Future.successful(Ok(ResponseImportContacts(immutable.Seq.empty[struct.User], 0, None)))
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
          usersFutureSeq <- Future.sequence(contactList.map { c => persist.User.findWithAvatar(c._1)(None) }).map(_.flatten) // TODO: OPTIMIZE!!!
        } yield {
          val localNames = immutable.HashMap(contactList :_*)
          val users = usersFutureSeq.map {
            case (user, avatarData) =>
              struct.User.fromModel(user, avatarData, authId, localNames.get(user.uid))
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
    persist.User.find(contactId)(None) flatMap {
      case Some(user) =>
        if (accessHash == ACL.userAccessHash(authId, contactId, user.accessSalt)) {
          val clFuture = persist.contact.UserContact.find(ownerUserId = currentUser.uid, contactUserId = contactId) flatMap {
            case Some(contact) =>
              persist.contact.UserContact.save(models.contact.UserContact(
                currentUser.uid, user.uid, user.phoneNumber, name, user.accessSalt
              ))
            case None =>
              addContactSendUpdate(currentUser, user.uid, user.phoneNumber, name, user.accessSalt)
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
          phones <- persist.UserPhone.findAllByNumbers(filteredPhones)
          usersAvatars <- Future.sequence(phones map (p => persist.User.findWithAvatar(p.userId)(None))).map(_.flatten)
        } yield Ok(ResponseSearchContacts(usersAvatars.map(ua => struct.User.fromModel(ua._1, ua._2, authId)).toIndexedSeq))
    }
  }

  def handleRequestAddContact(userId: Int, accessHash: Long): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    if (userId == currentUser.uid) {
      Future.successful(RpcErrors.cantAddSelf)
    } else {
      persist.User.find(userId)(None) flatMap {
        case Some(user) =>
          if (accessHash == ACL.userAccessHash(authId, userId, user.accessSalt)) {
            persist.contact.UserContact.find(ownerUserId = currentUser.uid, contactUserId = userId) flatMap {
              case None =>
                addContactSendUpdate(currentUser, user.uid, user.phoneNumber, "", user.accessSalt) map {
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
