package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.contact.{PublicKeyRequest, PublicKeyResponse, RequestPublicKeys, ResponsePublicKeys}
import com.secretapp.backend.services.{UserManagerService, GeneratorService}
import com.secretapp.backend.persist.UserPublicKeyRecord
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
    case RequestPublicKeys(keys) =>
      authorizedRequest {
        handleRequestPublicKeys(keys)
      }
  }

  def handleRequestPublicKeys(keys: immutable.Seq[PublicKeyRequest]): Future[RpcResponse] = {
    val authId = currentAuthId
    val keysMap = keys.map(k => k.uid * k.keyHash -> k.accessHash).toMap
    for {
      pkeys <- UserPublicKeyRecord.getEntitiesByPublicKeyHash(keys.map(k => (k.uid, k.keyHash)))
    } yield {
      val items = pkeys.filter { k =>
        val hkey = k.uid * k.publicKeyHash
        val ahash = ACL.userAccessHash(authId, k.uid, k.userAccessSalt)
        keysMap(hkey) == ahash
      }
      val pubKeys = items.map { key =>
        PublicKeyResponse(key.uid, key.publicKeyHash, key.publicKey)
      }
      Ok(ResponsePublicKeys(pubKeys))
    }
  }
}
