package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.util.ACL
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._
import com.websudos.util.testing.AsyncAssertionsHelper._
import java.security.MessageDigest
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

class ContactServiceSpec extends RpcSpec {
  implicit val transport = com.secretapp.backend.api.frontend.MTConnection // TODO

  "ContactService" should {
    "handle RPC import contacts request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val phones = immutable.Seq(
        PhoneToImport(contact.phoneNumber, s"${contact.name}_wow1".some),
        PhoneToImport(currentUser.phoneNumber, s"${currentUser.name}_wow1".some) /* check for filtered own phone number */
      )
      val emails = immutable.Seq[EmailToImport]()
      sendRpcMsg(RequestImportContacts(phones, emails))

      val (users, seq, state) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseImportedContacts => (r.users, r.seq, r.state)
      }
      users.should_==(Seq(struct.User.fromModel(contact, scope.authId, s"${contact.name}_wow1".some)))
    }

    "handle RPC import contacts requests" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contacts = (1 to 10).toList.map { _ =>
        genTestScopeWithUser().user
      }
      val emails = immutable.Seq[EmailToImport]()
      contacts foreach { contact =>
        sendRpcMsg(RequestImportContacts(immutable.Seq(PhoneToImport(contact.phoneNumber, None)), emails))
      }

      val users = contacts.zipWithIndex.map {
        case (_, index) =>
          expectRpcMsgByPF(withNewSession = index == 0) {
            case r: ResponseImportedContacts => r.users
          }
      }.flatten

      val sortedContactsId = contacts.map(_.uid).to[immutable.SortedSet]
      users.map(_.uid).to[immutable.SortedSet].should_==(sortedContactsId)

      val cacheEntity = persist.contact.UserContactsListCache.getEntity(currentUser.uid).sync().get
      cacheEntity.contactsId.size.should_==(sortedContactsId.size)
      cacheEntity.contactsId.to[immutable.SortedSet].should_==(sortedContactsId)
    }

    "handle RPC get contacts request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contacts = immutable.Seq(
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user
      )
      val contactsListTuple = contacts.zipWithIndex.map {
        case (c, index) => (struct.User.fromModel(c, scope.authId, s"${c.name}_$index".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      persist.contact.UserContactsList.insertNewContacts(currentUser.uid, contactsListTuple).sync()
      persist.contact.UserContactsListCache.addContactsId(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      sendRpcMsg(RequestGetContacts(persist.contact.UserContactsListCache.emptySHA1Hash))

      val (users, isChanged) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(true)
      users.toSet.should_==(contactsList.zipWithIndex.map {
        case (c, index) => c.copy(localName = s"${c.name}_$index".some)
      }.toSet)
    }

    "handle RPC get contacts request when isNotChanged" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contacts = immutable.Seq(
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user,
        genTestScopeWithUser().user
      )
      val contactsListTuple = contacts.zipWithIndex.map {
        case (c, index) => (struct.User.fromModel(c, scope.authId, s"${c.name}_$index".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      persist.contact.UserContactsList.insertNewContacts(currentUser.uid, contactsListTuple).sync()
      persist.contact.UserContactsListCache.addContactsId(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      val sha1Hash = persist.contact.UserContactsListCache.getSHA1Hash(contactsList.map(_.uid).toSet)
      sendRpcMsg(RequestGetContacts(sha1Hash))

      val (users, isChanged) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(false)
      users.isEmpty.should_==(true)
    }

    "handle RPC edit name contact request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val contacts = immutable.Seq(contact)
      val contactsListTuple = contacts.map { c =>
        (struct.User.fromModel(c, scope.authId, s"default_local_name".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      persist.contact.UserContactsList.insertNewContacts(currentUser.uid, contactsListTuple).sync()
      persist.contact.UserContactsListCache.addContactsId(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      sendRpcMsg(RequestEditContactName(contact.uid, ACL.userAccessHash(scope.authId, contact), "new_local_name"))

      // TODO
      val reqSeq = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => r
      }

      sendRpcMsg(RequestGetContacts(persist.contact.UserContactsListCache.emptySHA1Hash))
      val userLocalName = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users.headOption.map(_.localName).flatten
      }
      userLocalName.should_==("new_local_name".some)
    }

    "handle RPC delete contact request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val contacts = immutable.Seq(contact)
      val contactsListTuple = contacts.map { c =>
        (struct.User.fromModel(c, scope.authId), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      persist.contact.UserContactsList.insertNewContacts(currentUser.uid, contactsListTuple).sync()
      persist.contact.UserContactsListCache.addContactsId(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      sendRpcMsg(RequestDeleteContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))

      // TODO
      val reqSeq = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => r
      }

      sendRpcMsg(RequestGetContacts(persist.contact.UserContactsListCache.emptySHA1Hash))
      val (users, isChanged) = expectRpcMsgByPF() {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(false)
      users.isEmpty.should_==(true)

      sendRpcMsg(RequestImportContacts(immutable.Seq(PhoneToImport(contact.phoneNumber, None)), immutable.Seq()))
      val importedUsers = expectRpcMsgByPF() {
        case r: ResponseImportedContacts => users
      }
      importedUsers.isEmpty.should_==(true)
    }

    "handle RPC find contacts request" in {
      implicit val scope = genTestScopeWithUser()
      val contact = genTestScopeWithUser().user
      val phoneUtil = PhoneNumberUtil.getInstance()
      val phone = phoneUtil.parse(contact.phoneNumber.toString, "RU")
      Set(PhoneNumberFormat.INTERNATIONAL, PhoneNumberFormat.NATIONAL, PhoneNumberFormat.E164).zipWithIndex.foreach {
        case (format, index) =>
          val searchString = phoneUtil.format(phone, format)
          sendRpcMsg(RequestFindContacts(searchString))

          val users = expectRpcMsgByPF(withNewSession = index == 0) {
            case r: ResponseFindContacts => r.users
          }
          users.should_==(Seq(struct.User.fromModel(contact, scope.authId)))
      }
    }

    "handle RPC add contacts request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      persist.contact.UserContactsListCache.insertEntity(currentUser.uid, Set(), Set(contact.uid)).sync()

      sendRpcMsg(RequestAddContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))
      expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq =>
      }

      val responseContacts = Seq(struct.User.fromModel(contact, scope.authId))

      sendRpcMsg(RequestGetContacts(persist.contact.UserContactsListCache.emptySHA1Hash))
      val importedContacts = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users
      }
      importedContacts.should_==(responseContacts)
    }
  }
}
