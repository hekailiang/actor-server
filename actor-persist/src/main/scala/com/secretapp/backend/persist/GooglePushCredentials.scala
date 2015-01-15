package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._

object GooglePushCredentials extends SQLSyntaxSupport[models.GooglePushCredentials] {
  override val tableName = "google_push_credentials"
  override val columnNames = Seq("auth_id", "project_id", "reg_id")

  lazy val gcreds = GooglePushCredentials.syntax("gcreds")

  def apply(gcreds: SyntaxProvider[models.GooglePushCredentials])(
    rs: WrappedResultSet
  ): models.GooglePushCredentials = apply(gcreds.resultName)(rs)

  def apply(gcreds: ResultName[models.GooglePushCredentials])(
    rs: WrappedResultSet
  ): models.GooglePushCredentials = models.GooglePushCredentials(
    authId = rs.long(gcreds.authId),
    projectId = rs.long(gcreds.projectId),
    regId = rs.string(gcreds.regId)
  )

  def find(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = GooglePushCredentials.autoSession
  ): Future[Option[models.GooglePushCredentials]] = Future {
    blocking {
      withSQL {
        select.from(GooglePushCredentials as gcreds)
          .where.eq(gcreds.column("auth_id"), authId)
          .limit(1)
      }.map(GooglePushCredentials(gcreds)).single.apply
    }
  }

  def existsSync(authId: Long)(
    implicit session: DBSession
  ): Boolean =
    sql"""
      select exists (
        select 1 from google_push_credentials where auth_id = ${authId}
      )
      """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)

  def createOrUpdate(authId: Long, projectId: Long, regId: String)(
    implicit ec: ExecutionContext, session: DBSession = GooglePushCredentials.autoSession
  ): Future[models.GooglePushCredentials] = Future {
    blocking {
      val isRemoved = withSQL {
        delete.from(GooglePushCredentials)
          .where.eq(column.regId, regId)
      }.execute.apply

      existsSync(authId) match {
        case true =>
          withSQL {
            update(GooglePushCredentials).set(
              column.projectId -> projectId,
              column.regId -> regId
            )
              .where.eq(column.authId, authId)
          }.update.apply
        case false =>
          withSQL {
            insert.into(GooglePushCredentials).namedValues(
              column.authId -> authId,
              column.projectId -> projectId,
              column.regId -> regId
            )
          }.execute.apply
      }

      models.GooglePushCredentials(authId, projectId, regId)
    }
  }

  def destroy(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = GooglePushCredentials.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(GooglePushCredentials)
          .where.eq(column.authId, authId)
      }.execute.apply
    }
  }
}
