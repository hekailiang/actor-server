package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import scala.concurrent.Future

sealed class AuthIdRecord extends CassandraTable[AuthIdRecord, AuthId] {
  override lazy val tableName = "auth_ids"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object userId extends OptionalIntColumn(this) {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): AuthId = {
    AuthId(authId(row), userId(row))
  }

}

object AuthIdRecord extends AuthIdRecord with DBConnector {
  def insertEntity(item: AuthId)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.authId, item.authId)
      .value(_.userId, item.userId)
      .future()
  }

  def getEntity(authId: Long)(implicit session: Session): Future[Option[AuthId]] = {
    select.where(_.authId eqs authId).one()
  }
}
