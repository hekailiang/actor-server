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

  val userDatasCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Seq[(Long, models.UserData)]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  // TODO: optimize this helpers

  def getUserDatas(userId: Int): Future[Seq[(Long, models.UserData)]] = {
    Option(userDatasCache.get(userId)) match {
      case Some(userDatas) =>
        Future.successful(userDatas)
      case None =>
        persist.UserPublicKey.findAllByUserId(userId) flatMap { keys =>
          keys match {
            case firstKey :: _ =>
              for {
                userDataOpt <- persist.User.findData(userId)
              } yield userDataOpt map { userData =>
                keys map { key =>
                  (
                    key.hash,
                    userData
                  )
                }
              } getOrElse (Seq.empty)
            case Nil => Future.successful(Seq.empty)
          }
        }
    }
  }

  def getUserStruct(userId: Int, currentAuthId: Long, currentUserId: Int)(implicit s: ActorSystem): Future[Option[struct.User]] = {
    val userDataOptFuture = persist.User.findData(userId)
    val adOptFuture = persist.AvatarData.find(id = userId, typ = persist.AvatarData.typeVal[models.User])
    val localNameFuture = persist.contact.UserContact.findLocalName(ownerUserId = currentUserId, contactUserId = userId)

    for {
      userDataOpt <- userDataOptFuture
      adOpt <- adOptFuture
      localName <- localNameFuture
    } yield {
      userDataOpt map (
        struct.User.fromData(
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
      userDatas <- getUserDatas(userId)
    } yield {
      userDatas.headOption map { userData =>
        struct.UserOutPeer(userId, ACL.userAccessHash(authId, userData._2.id, userData._2.accessSalt))
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
