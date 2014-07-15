package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import scala.concurrent.Future

sealed class SessionIdRecord extends CassandraTable[SessionIdRecord, SessionId] {

  override lazy val tableName = "sessions"

  object authId extends LongColumn(this) with PartitionKey[Long]
  object sessionId extends LongColumn(this) with PartitionKey[Long]

  override def fromRow(row: Row): SessionId = {
    SessionId(authId(row), sessionId(row))
  }

}

object SessionIdRecord extends SessionIdRecord with DBConnector {

  def insertEntity(item: SessionId)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.authId, item.authId)
      .value(_.sessionId, item.sessionId)
      .future()
  }

  def getEntity(authId: Long, sessionId: Long)(implicit session: Session): Future[Option[SessionId]] = {
    select.where(_.authId eqs authId).and(_.sessionId eqs sessionId).one()
  }

}
