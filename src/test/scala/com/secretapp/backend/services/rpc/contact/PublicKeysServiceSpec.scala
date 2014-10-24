package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.services.rpc.RpcSpec
import scala.language.{ postfixOps, higherKinds }
import scala.collection.immutable
import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.secretapp.backend.persist._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.data.message.{RpcResponseBox, struct, RpcRequestBox}
import com.secretapp.backend.data.message.rpc.{Error, Ok, Request}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.crypto.ec
import com.websudos.util.testing.AsyncAssertionsHelper._
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import scodec.bits._
import scalaz._
import Scalaz._
import scala.util.Random

class PublicKeysServiceSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>
    "PublicKeysService" should {
      "return public keys" in {
        implicit val scope = genTestScope()

        val messageId = getMessageId()
        val publicKey = hex"ac1d".bits
        val publicKeyHash = ec.PublicKey.keyHash(publicKey)
        val name = "Timothy Klim"
        val clientPhoneId = rand.nextLong()
        val phoneNumber = genPhoneNumber()
        val user = User.build(uid = userId, authId = scope.authId, publicKey = publicKey, accessSalt = userSalt,
          phoneNumber = phoneNumber, name = name)
        authUser(user, phoneNumber)
        val secondUser = User.build(uid = userId + 1, authId = scope.authId + 1, publicKey = publicKey, accessSalt = userSalt,
          phoneNumber = phoneNumber + 1, name = name)
        val accessHash = User.getAccessHash(scope.authId, secondUser.uid, secondUser.accessSalt)
        UserRecord.insertEntityWithPhoneAndPK(secondUser).sync()

        val reqKeys = immutable.Seq(PublicKeyRequest(secondUser.uid, accessHash, secondUser.publicKeyHash))
        val rpcReq = RpcRequestBox(Request(RequestPublicKeys(reqKeys)))
        sendMsg(rpcReq)

        val resKeys = immutable.Seq(PublicKeyResponse(secondUser.uid, secondUser.publicKeyHash, secondUser.publicKey))
        expectRpcMsg(Ok(ResponsePublicKeys(resKeys)), withNewSession = true)
      }
    }
  }
}
