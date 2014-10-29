package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.services.rpc.RpcSpec
import scala.collection.immutable

class ContactServiceSpec extends RpcSpec {
  "ContactService" should {

    "handle RPC import contacts request" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        val contacts = immutable.Seq(ContactToImport(42, scope2.user.phoneNumber))
        val (response, _) = RequestImportContacts(contacts) :~> <~:[ResponseImportedContacts]

        response should_== ResponseImportedContacts(
          immutable.Seq(struct.User.fromModel(scope2.user, scope.user.authId)),
          immutable.Seq(ImportedContact(42, scope2.user.uid)))
      }
    }

    "filter out self number" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        val contacts = immutable.Seq(
          ContactToImport(42, scope2.user.phoneNumber),
          ContactToImport(43, scope1.user.phoneNumber))
        val (response, _) = RequestImportContacts(contacts) :~> <~:[ResponseImportedContacts]

        response should_== ResponseImportedContacts(
          immutable.Seq(struct.User.fromModel(scope2.user, scope.user.authId)),
          immutable.Seq(ImportedContact(42, scope2.user.uid)))
      }
    }

  }
}
