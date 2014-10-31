package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.concurrent.Future

sealed class AuthIdRecord extends CassandraTable[AuthIdRecord, models.AuthId] {
  override val tableName = "auth_ids"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object userId extends OptionalIntColumn(this) {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): models.AuthId = models.AuthId(authId(row), userId(row))
}

object AuthIdRecord extends AuthIdRecord with DBConnector {
  def insertEntity(item: models.AuthId)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.authId, item.authId)
      .value(_.userId, item.userId)
      .future()

  def getEntity(authId: Long)(implicit session: Session): Future[Option[models.AuthId]] =
    select.where(_.authId eqs authId).one()

  def deleteEntity(authId: Long)(implicit session: Session): Future[ResultSet] =
    delete.where(_.authId eqs authId).future()

  def getEntityWithUser(authId: Long)
                       (implicit session: Session): Future[Option[(models.AuthId, Option[models.User])]] = {
    def user(a: models.AuthId): Future[Option[models.User]] =
      a.userId match {
        case Some(uid) => UserRecord.getEntity(uid, a.authId)
        case None => Future.successful(None)
      }

    getEntity(authId) flatMap {
      case Some(auth) => user(auth) map { u => Some(auth, u)}
      case None => Future.successful(None)
    }
  }
}
