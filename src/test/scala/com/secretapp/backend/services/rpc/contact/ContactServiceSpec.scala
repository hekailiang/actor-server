package com.secretapp.backend.services.rpc.contact

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message.rpc.{ Ok, Request }
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.{ RpcRequestBox, RpcResponseBox }
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist._
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.rpc.RpcSpec
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.language.postfixOps
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._

class ContactServiceSpec extends RpcSpec {
  import system.dispatcher

  "ContactService" should {

    "handle RPC import contacts request" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        val contacts = immutable.Seq(ContactToImport(42, scope2.user.phoneNumber))
        val response = RequestImportContacts(contacts) :~> <~:[ResponseImportedContacts]

        response should_== ResponseImportedContacts(
          immutable.Seq(scope2.user.toStruct(scope.user.authId)),
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
        val response = RequestImportContacts(contacts) :~> <~:[ResponseImportedContacts]

        response should_== ResponseImportedContacts(
          immutable.Seq(scope2.user.toStruct(scope.user.authId)),
          immutable.Seq(ImportedContact(42, scope2.user.uid)))
      }
    }

  }
}
