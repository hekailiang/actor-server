package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import scala.concurrent.Future

sealed class PhoneRecord extends CassandraTable[PhoneRecord, Phone] {
  override lazy val tableName = "phones"

  object number extends LongColumn(this) with PartitionKey[Long]
  object userId extends IntColumn(this) {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): Phone = {
    Phone(number(row), userId(row))
  }
}

object PhoneRecord extends PhoneRecord with DBConnector {
  def insertEntity(entity: Phone)(implicit session: Session): Future[ResultSet] = entity match {
    case Phone(number, userId) =>
      insert.value(_.number, number)
        .value(_.userId, userId)
        .future()
  }

  def getEntity(number: Long)(implicit session: Session): Future[Option[Phone]] = {
    select.where(_.number eqs number).one()
  }
}
