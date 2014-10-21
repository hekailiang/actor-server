package com.secretapp.backend.persist

import com.datastax.driver.core.{Session, ResultSet, Row}
import com.secretapp.backend.data.models.GooglePushCredentials
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}

import scala.concurrent.Future

sealed class GooglePushCredentialsRecord extends CassandraTable[GooglePushCredentialsRecord, GooglePushCredentials] {

  override lazy val tableName = "google_push_credentials"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }

  object projectId extends LongColumn(this) {
    override lazy val name = "project_id"
  }

  object regId extends StringColumn(this) with Index[String] {
    override lazy val name = "reg_id"
  }

  override def fromRow(r: Row): GooglePushCredentials =
    GooglePushCredentials(authId(r), projectId(r), regId(r))
}

object GooglePushCredentialsRecord extends GooglePushCredentialsRecord with DBConnector {

  def set(c: GooglePushCredentials)(implicit s: Session): Future[ResultSet] = {
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

  def get(authId: Long)(implicit s: Session): Future[Option[GooglePushCredentials]] =
    select
      .where(_.authId eqs authId)
      .one()
}
