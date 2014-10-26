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
    * @return tuple containing sequences of (authId, key): new keys, removed keys and invalid keys
    */
  def fetchAuthIdsAndCheckKeysFor(userId: Int, keys: Seq[EncryptedAESKey], skipKeyHash: Option[Long] = None): Future[(Seq[(Long, EncryptedAESKey)], Seq[UserKey], Seq[UserKey], Seq[UserKey])] = {
    case class WithRemovedAndInvalid(good: Seq[(Long, EncryptedAESKey)], removed: Seq[Long], invalid: Seq[Long])

    UserPublicKeyRecord.fetchAllAuthIdsMap(userId) map { authIdsMap =>
      val withoutNew = keys.foldLeft(WithRemovedAndInvalid(Seq.empty, Seq.empty, Seq.empty)) {
        case (res, key) =>
          authIdsMap.get(key.keyHash) match {
            case Some(\/-(authId)) =>
              res.copy(good = res.good :+ (authId, key))
            case Some(-\/(authId)) =>
              res.copy(removed = res.removed :+ key.keyHash)
            case None =>
              res.copy(invalid = res.invalid :+ key.keyHash)
          }
      }

      val allKeys = skipKeyHash match {
        case Some(keyHash) =>
          authIdsMap.keySet - keyHash
        case None =>
          authIdsMap.keySet
      }

      (
        withoutNew.good,
        allKeys.diff(keys.map(_.keyHash).toSet).toSeq map (UserKey(userId, _)),
        withoutNew.removed map (UserKey(userId, _)),
        withoutNew.invalid map (UserKey(userId, _))
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
