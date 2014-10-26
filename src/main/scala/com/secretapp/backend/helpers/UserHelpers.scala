package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.message.struct.{ UserId, UserKey }
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESKey
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.UserPublicKeyRecord
import com.secretapp.backend.persist.UserRecord
import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz._
import Scalaz._

trait UserHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.dispatcher

  // Caches userId -> accessHash associations
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Seq[(Long, User)]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  // TODO: optimize this helpers

  def getUsers(uid: Int): Future[Seq[(Long, User)]] = {
    Option(usersCache.get(uid)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        UserRecord.byUid(uid) map (
          _ map {user => (user.publicKeyHash, user) }
        )
    }
  }

  def getUserIdStruct(userId: Int, authId: Long): Future[Option[UserId]] = {
    for {
      users <- getUsers(userId)
    } yield {
      users.headOption map { user =>
        UserId(userId, user._2.accessHash(authId))
      }
    }
  }

  def getAuthIds(userId: Int): Future[Seq[Long]] = {
    UserPublicKeyRecord.fetchAuthIdsByUserId(userId)
  }

  /**
    * Fetches authIds for userId and his key hashes
    *
    * @param userId user id
    * @param keyHashes key hashes
    * @return tuple containing Seq of (authId, key), new keys and invalid (possibly removed) keys
    */
  def fetchAuthIdsAndChangedKeysFor[A](userId: Int, keys: Seq[EncryptedAESKey]): Future[(Seq[(Long, EncryptedAESKey)], Seq[UserKey], Seq[UserKey])] = {
    UserPublicKeyRecord.fetchAuthIdsMap(userId) map { authIdsMap =>
      val withoutOld = keys.foldLeft(Tuple2(Seq.empty[(Long, EncryptedAESKey)], Seq.empty[Long])) {
        case (res, key) =>
          authIdsMap.get(key.keyHash) match {
            case Some(authId) =>
              (res._1 :+ (authId, key), res._2)
            case None =>
              (res._1, res._2 :+ key.keyHash)
          }
      }

      (
        withoutOld._1,
        authIdsMap.keySet.diff(keys.map(_.keyHash).toSet).toSeq map (UserKey(userId, _)),
        withoutOld._2 map (UserKey(userId, _))
      )
    }
  }

  // fetchAuthIdsMap
  def getAuthIdsAndKeyHashes(userId: Int): Future[Map[Long, Long]] = {
    UserPublicKeyRecord.fetchAuthIdsMap(userId)
  }

  def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long \/ Long]] = {
    UserPublicKeyRecord.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
  }
}
