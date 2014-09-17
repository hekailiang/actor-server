package com.secretapp.backend.persist

import com.datastax.driver.core.{Session, Row}
import com.secretapp.backend.data.models.GooglePushCredentials
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}

import scala.concurrent.Future
import scala.Function.const

sealed class GooglePushCredentialsRecord extends CassandraTable[GooglePushCredentialsRecord, GooglePushCredentials] {

  override lazy val tableName = "google_push_credentials"

  object userId extends IntColumn(this) with PartitionKey[Int]

  object authId extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "auth_id"
  }

  object projectId extends IntColumn(this) {
    override lazy val name = "project_id"
  }

  object token extends StringColumn(this)

  override def fromRow(r: Row): GooglePushCredentials =
    GooglePushCredentials(userId(r), authId(r), projectId(r), token(r))
}

object GooglePushCredentialsRecord extends GooglePushCredentialsRecord with DBConnector {

  def set(c: GooglePushCredentials)(implicit s: Session): Future[Unit] =
    update
      .where(_.userId eqs c.userId).and(_.authId eqs c.authId)
      .modify(_.projectId setTo c.projectId).and(_.token setTo c.token)
      .future map const()

  def remove(userId: Int, authId: Long)(implicit s: Session): Future[Unit] =
    delete
      .where(_.userId eqs userId).and(_.authId eqs authId)
      .future map const()
}
