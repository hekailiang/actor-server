package com.secretapp.backend.persist

import com.datastax.driver.core.{ResultSet, Session, Row}
import com.secretapp.backend.data.models.UnregisteredContact
import com.websudos.phantom.Implicits._

import scala.concurrent.Future

sealed class UnregisteredContactRecord extends CassandraTable[UnregisteredContactRecord, UnregisteredContact] {

  override val tableName = "unregistered_contacts"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }
  object authId extends LongColumn(this) {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): UnregisteredContact =
    UnregisteredContact(phoneNumber(row), authId(row))
}

object UnregisteredContactRecord extends UnregisteredContactRecord with DBConnector {
  def insertEntity(uc: UnregisteredContact)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.phoneNumber, uc.phoneNumber)
      .value(_.authId, uc.authId)
      .future

  def byNumber(phoneNumber: Long)(implicit session: Session): Future[Set[UnregisteredContact]] =
    select.where(_.phoneNumber eqs phoneNumber).fetch().map(_.toSet)
}
