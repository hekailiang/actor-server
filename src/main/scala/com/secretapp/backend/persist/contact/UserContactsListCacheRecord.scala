package com.secretapp.backend.persist.contact

import com.datastax.driver.core.{Session => CSession, ResultSet, Row}
import com.secretapp.backend.models.{ contact => models }
import com.secretapp.backend.persist.TableOps
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scodec.bits.BitVector
import java.security.MessageDigest

sealed class UserContactsListCacheRecord extends CassandraTable[UserContactsListCacheRecord, models.UserContactsListCache] {
  override lazy val tableName = "user_contacts_list_cache"

  object ownerId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "owner_id"
  }
  object sha1Hash extends StringColumn(this) {
    override lazy val name = "sha_hash"
  }
  object contactsId extends SetColumn[UserContactsListCacheRecord, models.UserContactsListCache, Int](this) {
    override lazy val name = "contacts_id"
  }
  object deletedContactsId extends SetColumn[UserContactsListCacheRecord, models.UserContactsListCache, Int](this) {
    override lazy val name = "deleted_contacts_id"
  }

  override def fromRow(row: Row): models.UserContactsListCache = {
    models.UserContactsListCache(
      ownerId = ownerId(row),
      sha1Hash = sha1Hash(row),
      contactsId = contactsId(row),
      deletedContactsId = deletedContactsId(row)
    )
  }
}

object UserContactsListCacheRecord extends UserContactsListCacheRecord with TableOps {
  import scalaz._
  import Scalaz._

  private def getSHA1Hash(ids: immutable.Set[Int]): String = {
    val uids = ids.to[immutable.SortedSet].mkString(",")
    val digest = MessageDigest.getInstance("SHA-256")
    BitVector(digest.digest(uids.getBytes)).toHex
  }

  lazy val emptySHA1Hash = getSHA1Hash(Set())

  def updateContactsId(userId: Int, newContactsId: immutable.Set[Int])(implicit csession: CSession): Future[ResultSet] = {
    select(_.contactsId).where(_.ownerId eqs userId).one().flatMap {
      case Some(oldContactsId) =>
        update
          .where(_.ownerId eqs userId)
          .modify(_.contactsId addAll newContactsId)
          .and(_.sha1Hash setTo getSHA1Hash(oldContactsId ++ newContactsId))
          .future()
      case None =>
        insert
          .value(_.ownerId, userId)
          .value(_.sha1Hash, getSHA1Hash(newContactsId))
          .value(_.contactsId, newContactsId)
          .future()
    }
  }

  def removeContact(userId: Int, contactId: Int)(implicit csession: CSession): Future[ResultSet] = {
    getContactsId(userId) flatMap { contactsId =>
      update
        .where(_.ownerId eqs userId)
        .modify(_.contactsId remove contactId)
        .and(_.deletedContactsId add contactId)
        .and(_.sha1Hash setTo getSHA1Hash(contactsId - contactId))
        .future()
    }
  }

  def getEntity(userId: Int)(implicit csession: CSession): Future[Option[models.UserContactsListCache]] = {
    select.where(_.ownerId eqs userId).one()
  }

  def getContactsId(userId: Int)(implicit csession: CSession): Future[immutable.HashSet[Int]] = {
    select(_.contactsId).where(_.ownerId eqs userId).one().map {
      case Some(contactsId) => contactsId.to[immutable.HashSet]
      case _ => immutable.HashSet[Int]()
    }
  }

  def getContactsAndDeletedId(userId: Int)(implicit csession: CSession): Future[immutable.HashSet[Int]] = {
    select(_.contactsId, _.deletedContactsId).where(_.ownerId eqs userId).one().map {
      case Some((contactsId, deletedContactsId)) => (contactsId ++ deletedContactsId).to[immutable.HashSet]
      case _ => immutable.HashSet[Int]()
    }
  }

  def getSHA1HashOrDefault(userId: Int)(implicit csession: CSession): Future[String] = {
    select(_.sha1Hash).where(_.ownerId eqs userId).one().map {
      case Some(sha1Hash) => sha1Hash
      case _ => emptySHA1Hash
    }
  }
}
