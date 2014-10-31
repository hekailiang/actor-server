package com.secretapp.backend.persist

import com.datastax.driver.core.{ Session => CSession }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.concurrent.Future
import scala.concurrent.duration._

sealed class AuthSmsCodeRecord extends CassandraTable[AuthSmsCodeRecord, models.AuthSmsCode] {
  override val tableName = "auth_sms_codes"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }
  object smsHash extends StringColumn(this) { // TODO: with PartitionKey[Long]
    override lazy val name = "sms_hash"
  }
  object smsCode extends StringColumn(this) {
    override lazy val name = "sms_code"
  }

  override def fromRow(row: Row): models.AuthSmsCode =
    models.AuthSmsCode(phoneNumber(row), smsHash(row), smsCode(row))
}

object AuthSmsCodeRecord extends AuthSmsCodeRecord with DBConnector {
  def insertEntity(entity: models.AuthSmsCode)(implicit session: CSession): Future[ResultSet] =
    insert
      .value(_.phoneNumber, entity.phoneNumber)
      .value(_.smsHash, entity.smsHash)
      .value(_.smsCode, entity.smsCode)
      .ttl(15.minutes.toSeconds.toInt)
      .future()

  def getEntity(phoneNumber: Long)(implicit session: CSession): Future[Option[models.AuthSmsCode]] =
    select.where(_.phoneNumber eqs phoneNumber).one()

  def dropEntity(phoneNumber: Long)(implicit session: CSession) =
    delete.where(_.phoneNumber eqs phoneNumber).future()
}
