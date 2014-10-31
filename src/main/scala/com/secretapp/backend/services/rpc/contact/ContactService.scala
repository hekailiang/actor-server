package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{UpdatesBroker, SocialProtocol, ApiBrokerService}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models.contact
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
    val authId = currentAuthId
    val currentUser = getUser.get
    val filteredPhones = phones.filter(_.phoneNumber != currentUser.phoneNumber) // TODO: remove user.get
    val phoneNumbers = filteredPhones.map(_.phoneNumber).toSet
    val phonesMap = immutable.HashMap(filteredPhones.map { p => p.phoneNumber -> p.contactName } :_*)
    val usersSeq = for {
      phones <- PhoneRecord.getEntities(phoneNumbers)
      existsContactsId <- UserContactsListCacheRecord.getContactsId(currentUser.uid)
      uniquePhones = phones.filter(p => !existsContactsId.contains(p.userId))
      usersFutureSeq <- Future.sequence(uniquePhones map (p => UserRecord.getEntity(p.userId))).map(_.flatten) // TODO: OPTIMIZE!!!
    } yield {
      val t = usersFutureSeq.foldLeft(immutable.Seq[struct.User](), immutable.Set[Int](), immutable.Set[Long]()) {
        case ((users, newContactsId, registeredPhones), user) =>
          (users :+ struct.User.fromModel(user, authId, phonesMap(user.phoneNumber)),
            newContactsId + user.uid,
            registeredPhones + user.phoneNumber)
      }
      (t, existsContactsId)
    }

    usersSeq flatMap {
      case ((users, newContactsId, registeredPhones), existsContactsId) =>
        if (users.nonEmpty) {
          newContactsId foreach {
            socialBrokerRegion ! SocialMessageBox(_, RelationsNoted(Set(currentUser.uid))) // TODO: wrap as array!
          }

          (phoneNumbers &~ registeredPhones).foreach { phoneNumber => // TODO: move into singleton method
            UnregisteredContactRecord.insertEntity(models.UnregisteredContact(phoneNumber, currentUser.uid))
          }

          val clFuture = UserContactsListRecord.insertEntities(currentUser.uid, users)
          val clCacheFuture = UserContactsListCacheRecord.upsertEntity(currentUser.uid, existsContactsId ++ newContactsId)
          val stateFuture = ask(
            updatesBrokerRegion,
            UpdatesBroker.NewUpdatePush(currentUser.authId,
              updateProto.contact.UpdateContactsAdded(newContactsId.toIndexedSeq))
          ).mapTo[UpdatesBroker.StrictState].map {
            case (seq, state) => Ok(ResponseImportedContacts(users, seq, uuid.encodeValid(state)))
          }

          for {
            _ <- clFuture
            _ <- clCacheFuture
            response <- stateFuture
          } yield response
        } else Future.successful(Ok(ResponseImportedContacts(users, 0, BitVector.empty)))
    }
  }

  def handleRequestGetContacts(contactsHash: String): Future[RpcResponse] = {
    val authId = currentAuthId
    val currentUser = getUser.get
    UserContactsListCacheRecord.getSHA1HashOrDefault(currentUser.uid) flatMap { sha1Hash =>
      if (contactsHash == sha1Hash)
        Future.successful(Ok(ResponseGetContacts(immutable.Seq[struct.User](), isNotChanged = true)))
      else {
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
}
