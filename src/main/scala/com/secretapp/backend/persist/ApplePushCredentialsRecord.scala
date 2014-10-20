package com.secretapp.backend.persist

import com.datastax.driver.core.{Session, ResultSet, Row}
import com.secretapp.backend.data.models.ApplePushCredentials
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}

import scala.concurrent.Future

sealed class ApplePushCredentialsRecord extends CassandraTable[ApplePushCredentialsRecord, ApplePushCredentials] {

  override lazy val tableName = "apple_push_credentials"

  object uid extends IntColumn(this) with PartitionKey[Int]

  object authId extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "auth_id"
  }

  object apnsKey extends IntColumn(this) {
    override lazy val name = "apns_key"
  }

  object token extends StringColumn(this) {
    override lazy val name = "tok"
  }

  override def fromRow(r: Row): ApplePushCredentials =
    ApplePushCredentials(uid(r), authId(r), apnsKey(r), token(r))
}

object ApplePushCredentialsRecord extends ApplePushCredentialsRecord with DBConnector {

  def set(c: ApplePushCredentials)(implicit s: Session): Future[ResultSet] =
    update
      .where(_.uid eqs c.uid).and(_.authId eqs c.authId)
      .modify(_.apnsKey setTo c.apnsKey).and(_.token setTo c.token)
      .future

  def remove(uid: Int, authId: Long)(implicit s: Session): Future[ResultSet] =
    delete
      .where(_.uid eqs uid).and(_.authId eqs authId)
      .future

  def get(uid: Int, authId: Long)(implicit s: Session): Future[Option[ApplePushCredentials]] =
    select
      .where(_.uid eqs uid).and(_.authId eqs authId)
      .one()
}
