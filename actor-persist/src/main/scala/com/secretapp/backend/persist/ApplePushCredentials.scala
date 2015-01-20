package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._

object ApplePushCredentials extends SQLSyntaxSupport[models.ApplePushCredentials] {
  override val tableName = "apple_push_credentials"
  override val columnNames = Seq("auth_id", "apns_key", "token")

  lazy val acreds = ApplePushCredentials.syntax("acreds")

  def apply(acreds: SyntaxProvider[models.ApplePushCredentials])(
    rs: WrappedResultSet
  ): models.ApplePushCredentials = apply(acreds.resultName)(rs)

  def apply(acreds: ResultName[models.ApplePushCredentials])(
    rs: WrappedResultSet
  ): models.ApplePushCredentials = models.ApplePushCredentials(
    authId = rs.long(acreds.authId),
    apnsKey = rs.int(acreds.apnsKey),
    token = rs.string(acreds.token)
  )

  def find(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = ApplePushCredentials.autoSession
  ): Future[Option[models.ApplePushCredentials]] = Future {
    blocking {
      withSQL {
        select.from(ApplePushCredentials as acreds)
          .where.eq(acreds.column("auth_id"), authId)
          .limit(1)
      }.map(ApplePushCredentials(acreds)).single.apply
    }
  }

  def existsSync(authId: Long)(
    implicit session: DBSession
  ): Boolean =
    sql"""
      select exists (
        select 1 from apple_push_credentials where auth_id = ${authId}
      )
      """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)

  def createOrUpdate(authId: Long, apnsKey: Int, token: String)(
    implicit ec: ExecutionContext, session: DBSession = ApplePushCredentials.autoSession
  ): Future[models.ApplePushCredentials] = Future {
    blocking {
      val isRemoved = withSQL {
        delete.from(ApplePushCredentials)
          .where.eq(column.token, token)
      }.execute.apply

      existsSync(authId) match {
        case true =>
          withSQL {
            update(ApplePushCredentials).set(
              column.apnsKey -> apnsKey,
              column.token -> token
            )
              .where.eq(column.authId, authId)
          }.update.apply
        case false =>
          withSQL {
            insert.into(ApplePushCredentials).namedValues(
              column.authId -> authId,
              column.apnsKey -> apnsKey,
              column.token -> token
            )
          }.execute.apply
      }

      models.ApplePushCredentials(authId, apnsKey, token)
    }
  }

  def destroy(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = ApplePushCredentials.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(ApplePushCredentials)
          .where.eq(column.authId, authId)
      }.execute.apply
    }
  }
}
