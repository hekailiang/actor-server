package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.types._
import scodec.bits.BitVector
import scala.concurrent.Future
import scala.collection.immutable
import scalaz._
import Scalaz._

sealed class GroupRecord extends CassandraTable[GroupRecord, Group] {
  override lazy val tableName = "group_groups"

  object id extends IntColumn(this) with PartitionKey[Int]

  object creatorUserId extends IntColumn(this) {
    override lazy val name = "creator_user_id"
  }
  object accessHash extends LongColumn(this) {
    override lazy val name = "access_hash"
  }
  object title extends StringColumn(this)
  object keyHash extends BlobColumn(this) {
    override lazy val name = "key_hash"
  }
  object publicKey extends BlobColumn(this) {
    override lazy val name = "public_key"
  }

  override def fromRow(row: Row): Group = {
    Group(
      id            = id(row),
      creatorUserId = creatorUserId(row),
      accessHash    = accessHash(row),
      title         = title(row),
      keyHash       = BitVector(keyHash(row)),
      publicKey     = BitVector(publicKey(row))
    )
  }
}

object GroupRecord extends GroupRecord with DBConnector {
  def insertEntity(entity: Group)(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.id, entity.id)
      .value(_.creatorUserId, entity.creatorUserId)
      .value(_.accessHash, entity.accessHash)
      .value(_.title, entity.title)
      .value(_.keyHash, entity.keyHash.toByteBuffer)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .future()
  }

  def getEntity(groupId: Int)(implicit session: Session): Future[Option[Group]] = {
    select.where(_.id eqs groupId).one()
  }

  def setTitle(id: Int, title: String)(implicit session: Session): Future[ResultSet] = {
    update.where(_.id eqs id).modify(_.title setTo title).future()
  }
}
