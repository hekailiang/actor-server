package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row }
import com.datastax.driver.core.{ Session => CSession }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models.AuthSmsCode
import scala.concurrent.Future

sealed class AuthSmsCodeRecord extends CassandraTable[AuthSmsCodeRecord, AuthSmsCode] {
  override lazy val tableName = "auth_sms_codes"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }
  object smsHash extends StringColumn(this) { // TODO: with PartitionKey[Long]
    override lazy val name = "sms_hash"
  }
  object smsCode extends StringColumn(this) {
    override lazy val name = "sms_code"
  }

  override def fromRow(row: Row): AuthSmsCode = {
    AuthSmsCode(phoneNumber(row), smsHash(row), smsCode(row))
  }
}

object AuthSmsCodeRecord extends AuthSmsCodeRecord with DBConnector {
  def insertEntity(entity: AuthSmsCode)(implicit session: CSession): Future[ResultSet] = {
    insert.value(_.phoneNumber, entity.phoneNumber)
      .value(_.smsHash, entity.smsHash)
      .value(_.smsCode, entity.smsCode)
      .future()
  }

  def getEntity(phoneNumber: Long)(implicit session: CSession): Future[Option[AuthSmsCode]] = {
    select.where(_.phoneNumber eqs phoneNumber).one()
  }
}
