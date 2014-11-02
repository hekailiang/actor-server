package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.persist.contact._
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._
import com.websudos.util.testing.AsyncAssertionsHelper._
import java.security.MessageDigest

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
      val contactsList = contacts.zipWithIndex.map {
        case (c, index) => struct.User.fromModel(c, scope.authId, s"${c.name}_$index".some)
      }
      UserContactsListRecord.insertEntities(currentUser.uid, contactsList).sync()
      UserContactsListCacheRecord.upsertEntity(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      sendRpcMsg(RequestGetContacts(UserContactsListCacheRecord.emptySHA1Hash))

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
      val contactsList = contacts.zipWithIndex.map {
        case (c, index) => struct.User.fromModel(c, scope.authId, s"${c.name}_$index".some)
      }
      UserContactsListRecord.insertEntities(currentUser.uid, contactsList).sync()
      UserContactsListCacheRecord.upsertEntity(currentUser.uid, contactsList.map(_.uid).toSet).sync()
      val uids = contactsList.map(_.uid).to[immutable.SortedSet].mkString(",")
      val digest = MessageDigest.getInstance("SHA-256")
      val sha1Hash = BitVector(digest.digest(uids.getBytes)).toHex
      sendRpcMsg(RequestGetContacts(sha1Hash))

      val (users, isChanged) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseGetContacts => (r.users, !r.isNotChanged)
      }
      isChanged.should_==(false)
      users.isEmpty.should_==(true)
    }
  }
}
