package com.secretapp.backend.persist.contact

import com.datastax.driver.core.{ ResultSet, Row, Session => CSession }
import com.secretapp.backend.models.{ contact => models }
import com.secretapp.backend.persist.DBConnector
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

  override def fromRow(row: Row): models.UserContactsListCache = {
    models.UserContactsListCache(
      ownerId = ownerId(row),
      sha1Hash = sha1Hash(row),
      contactsId = contactsId(row)
    )
  }
}

object UserContactsListCacheRecord extends UserContactsListCacheRecord with DBConnector {
  import scalaz._
  import Scalaz._

  def upsertEntity(userId: Int, contactsId: immutable.Set[Int])(implicit csession: CSession): Future[ResultSet] = {
    val uids = contactsId.to[immutable.SortedSet].mkString(",")
    val digest = MessageDigest.getInstance("SHA-256")
    val sha1Hash = BitVector(digest.digest(uids.getBytes)).toHex
    insert
      .value(_.ownerId, userId)
      .value(_.sha1Hash, sha1Hash)
      .value(_.contactsId, contactsId)
      .future()
  }

  def getContactsId(userId: Int)(implicit csession: CSession): Future[immutable.HashSet[Int]] = {
    select(_.contactsId).where(_.ownerId eqs userId).one().map {
      case Some(uids) => immutable.HashSet[Int](uids.toSeq :_*)
      case _ => immutable.HashSet[Int]()
    }
  }
}
