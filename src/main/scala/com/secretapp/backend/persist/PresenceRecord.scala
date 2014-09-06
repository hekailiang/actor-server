package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.datastax.driver.core.{ ResultSet, Row, Session }
import scala.concurrent.Future

case class Presence(userId: Int)

object Presence {
  val ttl = 300 // 5 mins
}

sealed class PresenceRecord extends CassandraTable[PresenceRecord, Presence] {
  override lazy val tableName = "presences"

  object userId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): Presence = {
    Presence(userId(row))
  }
}

object PresenceRecord extends PresenceRecord with DBConnector {
  def insertEntity(presence: Presence)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.userId, presence.userId).ttl(Presence.ttl).future()
  }
}
