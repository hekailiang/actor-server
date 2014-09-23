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
    "handle RPC request import contacts(old)" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      implicit val authId = rand.nextLong

      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val name = "Timothy Klim"
      val clientPhoneId = rand.nextLong()
      val user = User.build(uid = userId, authId = authId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = defaultPhoneNumber, name = name)
      authUser(user, defaultPhoneNumber)
      val secondUser = User.build(uid = userId + 1, authId = authId + 1, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = defaultPhoneNumber + 1, name = name)
      UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

      val reqContacts = immutable.Seq(ContactToImport(clientPhoneId, defaultPhoneNumber + 1))
      val rpcReq = RpcRequestBox(Request(RequestImportContacts(reqContacts)))
      val packageBlob = pack(0, authId, MessageBox(messageId, rpcReq))
      send(packageBlob)

      val resContacts = immutable.Seq(ImportedContact(clientPhoneId, secondUser.uid))
      val resBody = ResponseImportedContacts(immutable.Seq(secondUser.toStruct(authId)), resContacts)
      val rpcRes = RpcResponseBox(messageId, Ok(resBody))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "handle RPC import contacts request" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1
        val contacts = immutable.Seq(ContactToImport(42, scope2.user.phoneNumber))
        val response = RequestImportContacts(contacts) :~> <~:[ResponseImportedContacts]

        response should_== ResponseImportedContacts(
          immutable.Seq(scope2.user.toStruct(scope.user.authId)),
          immutable.Seq(ImportedContact(42, scope2.user.uid)))
      }
    }
  }
}
