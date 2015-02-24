package com.secretapp.backend.helpers

import akka.actor._
import akka.event.LoggingAdapter
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESKey
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz._
import Scalaz._

trait UserHelpers {
  val context: ActorContext

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
        persist.UserPublicKey.findAllByUserId(userId) flatMap { keys =>
          keys match {
            case firstKey :: _ =>
              for {
                userOpt <- persist.User.find(userId)(authId = Some(firstKey.authId))
              } yield userOpt map { user =>
                keys map { key =>
                  (
                    key.hash,
                    user.copy(
                      authId = key.authId,
                      publicKeyHash = key.hash,
                      publicKeyData = key.data
                    )
                  )
                }
              } getOrElse (Seq.empty)
            case Nil => Future.successful(Seq.empty)
          }
        }
    }
  }

  def getUserStruct(userId: Int, currentAuthId: Long, currentUserId: Int)(implicit s: ActorSystem): Future[Option[struct.User]] = {
    val userOptFuture = persist.User.find(userId)(None)
    val adOptFuture = persist.AvatarData.find(id = userId, typ = persist.AvatarData.typeVal[models.User])
    val localNameFuture = persist.contact.UserContact.findLocalName(ownerUserId = currentUserId, contactUserId = userId)

    for {
      userOpt <- userOptFuture
      adOpt <- adOptFuture
      localName <- localNameFuture
    } yield {
      userOpt map (
        struct.User.fromModel(
          _,
          adOpt.getOrElse(models.AvatarData.empty),
          currentAuthId,
          localName
        )
      )
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

  def getAuthIds(userId: Int): Future[Seq[Long]] = for {
    authIds <- persist.AuthId.findAllIdsByUserId(userId)
  } yield authIds

  def fetchAuthIdsForValidKeys(
    userId: Int,
    aesKeys: immutable.Seq[EncryptedAESKey],
    skipKeyHash: Option[Long]
  ): Future[(Vector[struct.UserKey], Vector[struct.UserKey], Vector[struct.UserKey]) \/ Vector[(EncryptedAESKey, Long)]] = {
    val keyHashes = aesKeys map (_.keyHash) toSet

    val activeAuthIdsMapFuture  = persist.UserPublicKey.findAllAuthIdsOfActiveKeys(userId) map (_.toMap)
    val deletedAuthIdsMapFuture = persist.UserPublicKey.findAllAuthIdsOfDeletedKeys(userId, keyHashes) map (_.toMap)

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
    persist.UserPublicKey.findAuthIdByUserIdAndHash(userId, publicKeyHash)
  }
}
