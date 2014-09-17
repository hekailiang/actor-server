package com.secretapp.backend.persist

import com.datastax.driver.core.{Session, Row}
import com.secretapp.backend.data.models.GooglePushCredentials
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}

import scala.concurrent.Future

sealed class GooglePushCredentialsRecord extends CassandraTable[GooglePushCredentialsRecord, GooglePushCredentials] {

  override lazy val tableName = "google_push_credentials"

  object uid extends IntColumn(this) with PartitionKey[Int]

  object authId extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "auth_id"
  }

  object projectId extends IntColumn(this) {
    override lazy val name = "project_id"
  }

  object regId extends StringColumn(this) {
    override lazy val name = "reg_id"
  }

  override def fromRow(r: Row): GooglePushCredentials =
    GooglePushCredentials(uid(r), authId(r), projectId(r), regId(r))
}

object GooglePushCredentialsRecord extends GooglePushCredentialsRecord with DBConnector {

  def set(c: GooglePushCredentials)(implicit s: Session): Future[Unit] =
    update
      .where(_.uid eqs c.uid).and(_.authId eqs c.authId)
      .modify(_.projectId setTo c.projectId).and(_.regId setTo c.regId)
      .future.mapTo[Unit]

  def remove(uid: Int, authId: Long)(implicit s: Session): Future[Unit] =
    delete
      .where(_.uid eqs uid).and(_.authId eqs authId)
      .future.mapTo[Unit]

  def get(uid: Int, authId: Long)(implicit s: Session): Future[Option[GooglePushCredentials]] =
    select
      .where(_.uid eqs uid).and(_.authId eqs authId)
      .one()
}
