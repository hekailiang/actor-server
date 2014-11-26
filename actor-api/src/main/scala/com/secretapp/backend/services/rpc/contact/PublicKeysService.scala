package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.contact.{ PublicKeyRequest, PublicKeyResponse, RequestGetPublicKeys, ResponseGetPublicKeys }
import com.secretapp.backend.services.{ UserManagerService, GeneratorService }
import com.secretapp.backend.persist
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait PublicKeysService {
  self: ApiBrokerService with GeneratorService with UserManagerService =>

  implicit val session: CSession

  import context._

  def handleRpcPublicKeys: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case RequestGetPublicKeys(keys) =>
      authorizedRequest {
        handleRequestGetPublicKeys(keys)
      }
  }

  def handleRequestGetPublicKeys(keys: immutable.Seq[PublicKeyRequest]): Future[RpcResponse] = {
    val authId = currentAuthId
    val keysMap = keys.map(k => k.userId * k.keyHash -> k.accessHash).toMap
    for {
      pkeys <- persist.UserPublicKey.getEntitiesByPublicKeyHash(keys.map(k => (k.userId, k.keyHash)))
    } yield {
      val items = pkeys.filter { k =>
        val hkey = k.userId * k.publicKeyHash
        val ahash = ACL.userAccessHash(authId, k.userId, k.userAccessSalt)
        keysMap(hkey) == ahash
      }
      val pubKeys = items.map { key =>
        PublicKeyResponse(key.userId, key.publicKeyHash, key.publicKey)
      }
      Ok(ResponseGetPublicKeys(pubKeys))
    }
  }
}
