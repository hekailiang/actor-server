package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._

import scala.concurrent.Future

sealed class UnregisteredContactRecord extends CassandraTable[UnregisteredContactRecord, models.UnregisteredContact] {

  override val tableName = "unregistered_contacts"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }

  object ownerUserId extends IntColumn(this) {
    override lazy val name = "owner_user_id"
  }

  override def fromRow(row: Row): models.UnregisteredContact =
    models.UnregisteredContact(phoneNumber(row), ownerUserId(row))
}

object UnregisteredContactRecord extends UnregisteredContactRecord with DBConnector {
  def insertEntity(uc: models.UnregisteredContact)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.phoneNumber, uc.phoneNumber)
      .value(_.ownerUserId, uc.ownerUserId)
      .future

  def byNumber(phoneNumber: Long)(implicit session: Session): Future[Set[models.UnregisteredContact]] =
    select.where(_.phoneNumber eqs phoneNumber).fetch().map(_.toSet)
}
