package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.Future
import scalikejdbc._, async._, FutureImplicits._

object AuthSmsCode extends SQLSyntaxSupport[models.AuthSmsCode] with ShortenedNames {
  override val tableName = "auth_sms_codes"
  override val columnNames = Seq(
    "phone_number", "sms_hash", "sms_code"
  )

  lazy val a = AuthSmsCode.syntax("auth_sms_codes")

  def apply(a: SyntaxProvider[models.AuthSmsCode])(rs: WrappedResultSet): models.AuthSmsCode = apply(a.resultName)(rs)

  def apply(a: ResultName[models.AuthSmsCode])(rs: WrappedResultSet): models.AuthSmsCode = models.AuthSmsCode(
    phoneNumber = rs.long(a.phoneNumber),
    smsHash = rs.string(a.smsHash),
    smsCode = rs.string(a.smsCode)
  )

  def findBy(where: SQLSyntax)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.AuthSmsCode]] = withSQL {
    select.from(AuthSmsCode as a)
      .where.append(sqls"${where}")
      .limit(1)
  } map (AuthSmsCode(a))

  def findByPhoneNumber(phoneNumber: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.AuthSmsCode]] = findBy(sqls.eq(a.phoneNumber, phoneNumber))

  def create(phoneNumber: Long, smsHash: String, smsCode: String)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.AuthSmsCode] = for {
    _ <- withSQL {
      insert.into(AuthSmsCode).namedValues(
        column.phoneNumber -> phoneNumber,
        column.smsHash -> smsHash,
        column.smsCode -> smsCode
      )
    }.execute.future
  } yield models.AuthSmsCode(phoneNumber, smsHash, smsCode)

  def destroy(phoneNumber: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Boolean] = withSQL {
    delete.from(AuthSmsCode)
      .where.eq(column.phoneNumber, phoneNumber)
  }.execute.future
}
