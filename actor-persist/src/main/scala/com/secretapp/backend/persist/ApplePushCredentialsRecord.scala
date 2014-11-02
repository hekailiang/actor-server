package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._

import scala.concurrent.Future

sealed class ApplePushCredentialsRecord extends CassandraTable[ApplePushCredentialsRecord, models.ApplePushCredentials] {

  override val tableName = "apple_push_credentials"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }

  object apnsKey extends IntColumn(this) {
    override lazy val name = "apns_key"
  }

  object token extends StringColumn(this) with Index[String] {
    override lazy val name = "tok"
  }

  override def fromRow(r: Row): models.ApplePushCredentials =
    models.ApplePushCredentials(authId(r), apnsKey(r), token(r))
}

object ApplePushCredentialsRecord extends ApplePushCredentialsRecord with TableOps {

  def set(c: models.ApplePushCredentials)(implicit s: Session): Future[ResultSet] = {
    @inline def doInsert() =
      insert.value(_.authId, c.authId).value(_.apnsKey, c.apnsKey).value(_.token, c.token).future()

    select(_.authId).where(_.token eqs c.token).one() flatMap {
      case Some(authId) =>
        delete.where(_.authId eqs authId).future() flatMap { _ =>
          doInsert()
        }
      case None =>
        doInsert()
    }
  }

  def remove(authId: Long)(implicit s: Session): Future[ResultSet] =
    delete
      .where(_.authId eqs authId)
      .future

  def get(authId: Long)(implicit s: Session): Future[Option[models.ApplePushCredentials]] =
    select
      .where(_.authId eqs authId)
      .one()
}
