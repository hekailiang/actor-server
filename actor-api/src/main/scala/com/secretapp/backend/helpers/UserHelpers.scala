package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.message.struct.{ UserOutPeer, UserKey }
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESKey
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.util.ACL
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
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Seq[(Long, models.User)]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  // TODO: optimize this helpers

  def getUsers(uid: Int): Future[Seq[(Long, models.User)]] = {
    Option(usersCache.get(uid)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        persist.User.byUid(uid) map (
          _ map {user => (user.publicKeyHash, user) }
        )
    }
  }

  def getUserIdStruct(userId: Int, authId: Long)(implicit s: ActorSystem): Future[Option[UserOutPeer]] = {
    for {
      users <- getUsers(userId)
    } yield {
      users.headOption map { user =>
        UserOutPeer(userId, ACL.userAccessHash(authId, user._2))
      }
    }
  }

  def getAuthIds(userId: Int): Future[Seq[Long]] = {
    persist.UserPublicKey.fetchAuthIdsByUserId(userId)
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

    persist.UserPublicKey.fetchAllAuthIdsMap(userId) map { authIdsMap =>
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

      val activeAuthIdsMap = authIdsMap filter {
        case (_, -\/(_)) => false
        case (_, \/-(_)) => true
      }

      val allKeys = skipKeyHash match {
        case Some(keyHash) =>
          activeAuthIdsMap.keySet - keyHash
        case None =>
          activeAuthIdsMap.keySet
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
    persist.UserPublicKey.fetchAuthIdsMap(userId)
  }

  def authIdFor(uid: Int, publicKeyHash: Long): Future[Option[Long \/ Long]] = {
    persist.UserPublicKey.getAuthIdByUidAndPublicKeyHash(uid, publicKeyHash)
  }
}
