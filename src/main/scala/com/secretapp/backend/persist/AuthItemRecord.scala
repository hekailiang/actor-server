package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.message.struct.AuthItem
import com.secretapp.backend.data.models._
import scala.concurrent.Future
import scodec.bits._

sealed class AuthItemRecord extends CassandraTable[AuthItemRecord, (AuthItem, Long, BitVector)] {
  override lazy val tableName = "auth_items"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }

  object id extends IntColumn(this) with PrimaryKey[Int]

  object deviceHash extends BlobColumn(this) {
    override lazy val name = "device_hash"
  }

  object authId extends LongColumn(this) with Index[Long] {
    override lazy val name = "auth_id"
  }

  object appId extends IntColumn(this) {
    override lazy val name = "app_id"
  }

  object appTitle extends StringColumn(this) {
    override lazy val name = "app_title"
  }

  object deviceTitle extends StringColumn(this) {
    override lazy val name = "device_title"
  }

  object authTime extends LongColumn(this) {
    override lazy val name = "auth_time"
  }

  object authLocation extends StringColumn(this) {
    override lazy val name = "auth_location"
  }

  object latitude extends OptionalDoubleColumn(this)

  object longitude extends OptionalDoubleColumn(this)

  override def fromRow(row: Row): (AuthItem, Long, BitVector) = {
    (
      AuthItem(
        id(row), appId(row), appTitle(row), deviceTitle(row), authTime(row),
        authLocation(row), latitude(row), longitude(row)
      ),
      authId(row),
      BitVector(deviceHash(row))
    )
  }
}

object AuthItemRecord extends AuthItemRecord with DBConnector {
  def insertEntity(item: AuthItem, userId: Int, authId: Long, deviceHash: BitVector)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.userId, userId).value(_.id, item.id).value(_.deviceHash, deviceHash.toByteBuffer).value(_.authId, authId)
      .value(_.appId, item.appId).value(_.appTitle, item.appTitle)
      .value(_.deviceTitle, item.deviceTitle).value(_.authTime, item.authTime)
      .value(_.authLocation, item.authLocation).value(_.latitude, item.latitude).value(_.longitude, item.longitude)
      .future()
  }

  def getEntity(userId: Int, id: Int)(implicit session: Session): Future[Option[(AuthItem, Long, BitVector)]] = {
    select.where(_.userId eqs userId).and(_.id eqs id).one()
  }

  def getEntityByUserIdAndAuthId(userId: Int, authId: Long)(implicit session: Session): Future[Option[(AuthItem, Long, BitVector)]] = {
    select.where(_.userId eqs userId).and(_.authId eqs authId).one()
  }

  def getEntities(userId: Int)(implicit session: Session): Future[Seq[(AuthItem, Long, BitVector)]] = {
    select.where(_.userId eqs userId).fetch()
  }

  def deleteEntity(userId: Int, id: Int)(implicit session: Session): Future[ResultSet] = {
    delete.where(_.userId eqs userId).and(_.id eqs id).future()
  }
}
