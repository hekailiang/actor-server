package com.secretapp.backend.helpers

import akka.actor._
import akka.event.LoggingAdapter
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.message.struct
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

  def log: LoggingAdapter

  import context.dispatcher

  // Caches userId -> accessHash associations
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Seq[(Long, models.User)]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  // TODO: optimize this helpers

  def getUsers(userId: Int): Future[Seq[(Long, models.User)]] = {
    Option(usersCache.get(userId)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        persist.User.byUid(userId) map (
          _ map {user => (user.publicKeyHash, user) }
        )
    }
  }

  def getUserStruct(userId: Int, authId: Long)(implicit s: ActorSystem): Future[Option[struct.User]] = {
    for (opt <- persist.User.getEntityWithAvatar(userId)) yield {
      opt map {
        case (user, avatarData) =>
          struct.User.fromModel(user, avatarData, authId)
      }
    }
  }

  def getUserIdStruct(userId: Int, authId: Long)(implicit s: ActorSystem): Future[Option[struct.UserOutPeer]] = {
    for {
      users <- getUsers(userId)
    } yield {
      users.headOption map { user =>
        struct.UserOutPeer(userId, ACL.userAccessHash(authId, user._2))
      }
    }
  }

  def getAuthIds(userId: Int): Future[Seq[Long]] = {
    persist.UserPublicKey.fetchAuthIdsByUserId(userId)
  }

  def fetchAuthIdsForValidKeys(
    userId: Int,
    aesKeys: immutable.Seq[EncryptedAESKey],
    skipKeyHash: Option[Long]
  ): Future[(Vector[struct.UserKey], Vector[struct.UserKey], Vector[struct.UserKey]) \/ Vector[(EncryptedAESKey, Long)]] = {
    val keyHashes = aesKeys map (_.keyHash) toSet

    val activeAuthIdsMapFuture  = persist.UserPublicKey.fetchAuthIdsOfActiveKeys(userId) map (_.toMap)
    val deletedAuthIdsMapFuture = persist.UserPublicKey.fetchAuthIdsOfDeletedKeys(userId, keyHashes) map (_.toMap)

    for {
      activeAuthIdsMap  <- activeAuthIdsMapFuture
      deletedAuthIdsMap <- deletedAuthIdsMapFuture
    } yield {
      val activeKeyHashes = activeAuthIdsMap.keySet
      val deletedKeyHashes = deletedAuthIdsMap.keySet
      val validKeyHashes = (activeKeyHashes ++ deletedKeyHashes)

      // TODO: optimize using keyHashes.foldLeft
      val newKeys = skipKeyHash
        .map(kh => activeKeyHashes.filterNot(_ == kh))
        .getOrElse(activeKeyHashes)
        .diff(keyHashes).toVector map (struct.UserKey(userId, _))

      log.debug(s"aesKeys=$aesKeys deletedAuthIdsMap=$deletedAuthIdsMap")

      val (goodAesKeys, removedKeys, invalidKeys) =
        aesKeys.foldLeft((Vector.empty[(EncryptedAESKey, Long)], Vector.empty[struct.UserKey], Vector.empty[struct.UserKey])) {
          case (res, aesKey) =>
            activeAuthIdsMap.get(aesKey.keyHash) match {
              case Some(authId) =>
                res.copy(_1 = res._1 :+ (aesKey, authId))
              case None =>
                if (deletedKeyHashes.contains(aesKey.keyHash)) {
                  res.copy(_2 = res._2 :+ struct.UserKey(userId, aesKey.keyHash))
                } else {
                  res.copy(_3 = res._3 :+ struct.UserKey(userId, aesKey.keyHash))
                }
            }
        }

      (newKeys, removedKeys, invalidKeys) match {
        case (Vector(), Vector(), Vector()) => goodAesKeys.right
        case x => x.left
      }
    }
  }

  def authIdFor(userId: Int, publicKeyHash: Long): Future[Option[Long \/ Long]] = {
    persist.UserPublicKey.getAuthIdByUidAndPublicKeyHash(userId, publicKeyHash)
  }
}
