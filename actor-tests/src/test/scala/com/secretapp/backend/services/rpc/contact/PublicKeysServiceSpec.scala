package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.language.{ postfixOps, higherKinds }
import scala.collection.immutable
import com.secretapp.backend.persist
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.message.rpc.{ Ok, Request }
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.models
import com.secretapp.backend.crypto.ec
import com.websudos.util.testing._
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
        val pkHash = ec.PublicKey.keyHash(publicKey)
        val user = models.User(userId, scope.authId, pkHash, publicKey, phoneNumber, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
        authUser(user, phoneNumber)
        val secondUser = models.User(userId + 1, scope.authId + 1, pkHash, publicKey, phoneNumber + 1, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
        val accessHash = ACL.userAccessHash(scope.authId, secondUser)
        persist.User.insertEntityWithChildren(secondUser).sync()

        val reqKeys = immutable.Seq(PublicKeyRequest(secondUser.uid, accessHash, secondUser.publicKeyHash))
        val rpcReq = RpcRequestBox(Request(RequestGetPublicKeys(reqKeys)))
        sendMsg(rpcReq)

        val resKeys = immutable.Seq(PublicKeyResponse(secondUser.uid, secondUser.publicKeyHash, secondUser.publicKey))
        expectRpcMsg(Ok(ResponseGetPublicKeys(resKeys)), withNewSession = true)
      }
    }
  }
}