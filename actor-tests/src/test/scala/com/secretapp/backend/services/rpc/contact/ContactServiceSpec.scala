package com.secretapp.backend.services.rpc.contact

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.helpers.ContactHelpers
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import com.websudos.util.testing._
import java.security.MessageDigest
import scala.collection.immutable
import scalaz.Scalaz._
import scodec.bits.BitVector

class ContactServiceSpec extends RpcSpec {
  implicit val transport = com.secretapp.backend.api.frontend.MTConnection // TODO

  "ContactService" should {
    "handle RPC import contacts request" in new sqlDb {
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
        case r: ResponseImportContacts => (r.users, r.seq, r.state)
      }
      users.should_==(Seq(struct.User.fromModel(contact, models.AvatarData.empty, scope.authId, s"${contact.name}_wow1".some)))
    }

    "handle RPC import contacts requests" in new sqlDb {
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
            case r: ResponseImportContacts => r.users
          }
      }.flatten

      val sortedContactsId = contacts.map(_.uid).to[immutable.SortedSet]
      users.map(_.uid).to[immutable.SortedSet].should_==(sortedContactsId)

      val contactIds = persist.contact.UserContact.findAllContactIds(currentUser.uid).sync()
      contactIds.size.should_==(sortedContactsId.size)
      contactIds.to[immutable.SortedSet].should_==(sortedContactsId)
    }

    "handle RPC get contacts request" in new sqlDb {
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
        case (c, index) => (struct.User.fromModel(c, models.AvatarData.empty, scope.authId, s"${c.name}_$index".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      ContactHelpers.createAllUserContacts(currentUser.uid, contactsListTuple).sync()
      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))

      val (users, isChanged) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(true)
      users.toSet.should_==(contactsList.zipWithIndex.map {
        case (c, index) => c.copy(localName = s"${c.name}_$index".some)
      }.toSet)
    }

    "handle RPC get contacts request when isNotChanged" in new sqlDb {
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
        case (c, index) => (struct.User.fromModel(c, models.AvatarData.empty, scope.authId, s"${c.name}_$index".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      ContactHelpers.createAllUserContacts(currentUser.uid, contactsListTuple).sync()
      val sha1Hash = persist.contact.UserContact.getSHA1Hash(contactsList.map(_.uid).toSet)
      sendRpcMsg(RequestGetContacts(sha1Hash))

      val (users, isChanged) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(false)
      users.isEmpty.should_==(true)
    }

    "handle RPC edit name contact request" in new sqlDb {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val contacts = immutable.Seq(contact)
      val contactsListTuple = contacts.map { c =>
        (struct.User.fromModel(c, models.AvatarData.empty, scope.authId, s"default_local_name".some), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      ContactHelpers.createAllUserContacts(currentUser.uid, contactsListTuple).sync()
      sendRpcMsg(RequestEditUserLocalName(contact.uid, ACL.userAccessHash(scope.authId, contact), "new_local_name"))

      // TODO
      val reqSeq = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => r
      }

      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))
      val userLocalName = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users.headOption.map(_.localName).flatten
      }
      userLocalName.should_==("new_local_name".some)
    }

    "handle RPC delete contact request" in new sqlDb {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val contacts = immutable.Seq(contact)
      val contactsListTuple = contacts.map { c =>
        (struct.User.fromModel(c, models.AvatarData.empty, scope.authId), c.accessSalt)
      }
      val contactsList = contactsListTuple.map(_._1)
      ContactHelpers.createAllUserContacts(currentUser.uid, contactsListTuple).sync()
      sendRpcMsg(RequestRemoveContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))

      // TODO
      val reqSeq = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => r
      }

      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))
      val (users, isChanged) = expectRpcMsgByPF() {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(false)
      users.isEmpty.should_==(true)

      sendRpcMsg(RequestImportContacts(immutable.Seq(PhoneToImport(contact.phoneNumber, None)), immutable.Seq()))
      val importedUsers = expectRpcMsgByPF() {
        case r: ResponseImportContacts => users
      }
      importedUsers.isEmpty.should_==(true)
    }

    "handle RPC find contacts request" in new sqlDb {
      implicit val scope = genTestScopeWithUser()
      val contact = genTestScopeWithUser().user
      val phoneUtil = PhoneNumberUtil.getInstance()
      val phone = phoneUtil.parse(contact.phoneNumber.toString, "RU")
      Set(PhoneNumberFormat.INTERNATIONAL, PhoneNumberFormat.NATIONAL, PhoneNumberFormat.E164).zipWithIndex.foreach {
        case (format, index) =>
          val searchString = phoneUtil.format(phone, format)
          sendRpcMsg(RequestSearchContacts(searchString))

          val users = expectRpcMsgByPF(withNewSession = index == 0) {
            case r: ResponseSearchContacts => r.users
          }
          users.should_==(Seq(struct.User.fromModel(contact, models.AvatarData.empty, scope.authId)))
      }
    }

    "handle RPC add contacts request" in new sqlDb {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user

      sendRpcMsg(RequestAddContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))
      val reqSeq = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => r
      }
      val responseContacts = Seq(struct.User.fromModel(contact, models.AvatarData.empty, scope.authId))

      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))
      val users = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users
      }
      users.should_==(responseContacts)

      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))
      val importedUsers = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users
      }
      importedUsers.should_==(responseContacts)

      sendRpcMsg(RequestRemoveContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))
      expectRpcMsgByPF() {
        case r: ResponseSeq => r
      }

      sendRpcMsg(RequestGetContacts(persist.contact.UserContact.emptySHA1Hash))
      val contactsUsers = expectRpcMsgByPF() {
        case r: ResponseGetContacts => r.users
      }
      contactsUsers.isEmpty.should_==(true)
    }
  }

  "RequestImportContacts handler" should {
    "not create UnregisteredContact entry if user with a phone is already registered" in new sqlDb {
      implicit val scope = genTestScopeWithUser()

      val currentUser = scope.user

      val contact = genTestScopeWithUser().user

      sendRpcMsg(RequestAddContact(contact.uid, ACL.userAccessHash(scope.authId, contact)))
      expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseSeq => ()
      }

      sendRpcMsg(RequestImportContacts(immutable.Seq(PhoneToImport(contact.phoneNumber, None)), immutable.Seq.empty))
      val contacts = expectRpcMsgByPF() {
        case r @ ResponseImportContacts(u, _, _) => u
      }

      contacts should_== Nil

      Thread.sleep(1000)

      persist.UnregisteredContact.findAllByPhoneNumber(contact.phoneNumber) should be_==(Nil).await
    }
  }
}
