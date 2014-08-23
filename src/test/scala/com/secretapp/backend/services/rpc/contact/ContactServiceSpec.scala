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
    "handle RPC request import contacts" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val clientPhoneId = rand.nextLong()
      val user = User.build(uid = userId, authId = mockAuthId, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber, firstName = firstName, lastName = lastName)
      authUser(user, phoneNumber)
      val secondUser = User.build(uid = userId + 1, authId = mockAuthId + 1, publicKey = publicKey, accessSalt = userSalt,
        phoneNumber = phoneNumber + 1, firstName = firstName, lastName = lastName)
      UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

      val reqContacts = immutable.Seq(ContactToImport(clientPhoneId, phoneNumber + 1))
      val rpcReq = RpcRequestBox(Request(RequestImportContacts(reqContacts)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val resContacts = immutable.Seq(ImportedContact(clientPhoneId, secondUser.uid))
      val resBody = ResponseImportedContacts(immutable.Seq(secondUser.toStruct(mockAuthId)), resContacts)
      val rpcRes = RpcResponseBox(messageId, Ok(resBody))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }
  }
}
