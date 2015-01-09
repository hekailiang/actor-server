package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import scala.concurrent.Future
import scalikejdbc._, async._, FutureImplicits._

object AuthId extends SQLSyntaxSupport[models.AuthId] with ShortenedNames {
  override val tableName = "auth_ids"
  override val columnNames = Seq("id", "user_id")

  lazy val a = AuthId.syntax("a")

  def apply(a: SyntaxProvider[models.AuthId])(rs: WrappedResultSet): models.AuthId = apply(a.resultName)(rs)

  def apply(a: ResultName[models.AuthId])(rs: WrappedResultSet): models.AuthId = models.AuthId(
    id = rs.long(a.id),
    userId = rs.intOpt(a.userId)
  )

  def find(id: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.AuthId]] = withSQL {
    select.from(AuthId as a).where.eq(a.id, id)
  }.map(AuthId(a))

  def findWithUser(id: Long)(
    implicit ec: EC, csession: CSession, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[(models.AuthId, Option[models.User])]] = {
    def user(authId: models.AuthId): Future[Option[models.User]] =
      authId.userId match {
        case Some(uid) => User.getEntity(uid, authId.id)
        case None => Future.successful(None)
      }

    find(id) flatMap {
      case Some(authId) => user(authId) map { u => Some(authId, u)}
      case None => Future.successful(None)
    }
  }

  def create(id: Long, userId: Option[Int])(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.AuthId] = for {
    _ <- withSQL {
      insert.into(AuthId).namedValues(
        column.id -> id,
        column.userId -> userId
      )
    }.execute.future
  } yield models.AuthId(id, userId)

  def save(authId: models.AuthId)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.AuthId] = withSQL {
    update(AuthId).set(
      column.userId -> authId.userId
    ).where.eq(column.id, authId.id)
  }.update.future.map(_ => authId)

  def createOrUpdate(id: Long, userId: Option[Int])(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.AuthId] = find(id) flatMap { // TODO: don't select all rows, it's just a row presence check
    case Some(_) => save(models.AuthId(id, userId))
    case None => create(id, userId)
  }

  def destroy(id: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Boolean] = withSQL {
    delete.from(AuthId).where.eq(column.id, id)
  }.execute.future
}
