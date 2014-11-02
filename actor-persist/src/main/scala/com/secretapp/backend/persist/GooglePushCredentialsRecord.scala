package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import scala.concurrent.Future

sealed class GooglePushCredentialsRecord extends CassandraTable[GooglePushCredentialsRecord, models.GooglePushCredentials] {

  override val tableName = "google_push_credentials"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object projectId extends LongColumn(this) {
    override lazy val name = "project_id"
  }
  object regId extends StringColumn(this) with Index[String] {
    override lazy val name = "reg_id"
  }

  override def fromRow(r: Row): models.GooglePushCredentials =
    models.GooglePushCredentials(authId(r), projectId(r), regId(r))
}

object GooglePushCredentialsRecord extends GooglePushCredentialsRecord with TableOps {

  def set(c: models.GooglePushCredentials)(implicit s: Session): Future[ResultSet] = {
    @inline def doInsert() =
      insert.value(_.authId, c.authId).value(_.projectId, c.projectId).value(_.regId, c.regId).future()

    select(_.authId).where(_.regId eqs c.regId).one() flatMap {
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

  def get(authId: Long)(implicit s: Session): Future[Option[models.GooglePushCredentials]] =
    select
      .where(_.authId eqs authId)
      .one()
}
