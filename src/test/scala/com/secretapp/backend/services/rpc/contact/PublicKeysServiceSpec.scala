package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.language.{ postfixOps, higherKinds }
import scala.collection.immutable
import com.secretapp.backend.persist._
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.rpc.{Ok, Request}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.models
import com.secretapp.backend.crypto.ec
import com.websudos.util.testing.AsyncAssertionsHelper._
import scodec.bits._

class PublicKeysServiceSpec extends RpcSpec {
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
        val user = models.User.build(uid = userId, authId = scope.authId, publicKey = publicKey, accessSalt = userSalt,
          phoneNumber = phoneNumber, name = name)
        authUser(user, phoneNumber)
        val secondUser = models.User.build(uid = userId + 1, authId = scope.authId + 1, publicKey = publicKey, accessSalt = userSalt,
          phoneNumber = phoneNumber + 1, name = name)
        val accessHash = ACL.userAccessHash(scope.authId, secondUser)
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
