package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import scala.concurrent.{ ExecutionContext, Future }
import scalikejdbc._

object AuthId extends SQLSyntaxSupport[models.AuthId] {
  override val tableName = "auth_ids"
  override val columnNames = Seq("id", "user_id")

  lazy val a = AuthId.syntax("a")

  def apply(a: SyntaxProvider[models.AuthId])(rs: WrappedResultSet): models.AuthId = apply(a.resultName)(rs)

  def apply(a: ResultName[models.AuthId])(rs: WrappedResultSet): models.AuthId = models.AuthId(
    id = rs.long(a.id),
    userId = rs.intOpt(a.userId)
  )

  def find(id: Long)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[Option[models.AuthId]] = Future {
    withSQL {
      select.from(AuthId as a).where.eq(a.id, id)
    }.map(AuthId(a)).single.apply
  }

  def findWithUser(id: Long)(
    implicit ec: ExecutionContext, csession: CSession, session: DBSession = AuthId.autoSession
  ): Future[Option[(models.AuthId, Option[models.User])]] = {
    def user(authId: models.AuthId): Future[Option[models.User]] =
      authId.userId match {
        case Some(userId) => User.find(userId)(Some(authId.id))
        case None => Future.successful(None)
      }

    find(id) flatMap {
      case Some(authId) => user(authId) map { u => Some(authId, u)}
      case None => Future.successful(None)
    }
  }

  // TODO: remove this method when we will move authId away from User model
  // it is used in User.find
  def findFirstIdByUserId(userId: Long)(
    implicit ec: ExecutionContext, csession: CSession, session: DBSession = AuthId.autoSession
  ): Future[Option[Long]] = Future {
    withSQL {
      select.from(AuthId as a)
        .where.eq(a.userId, userId).limit(1)
    }.map(rs => rs.long(a.resultName.id)).single.apply
  }

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession): Future[List[models.AuthId]] =
    Future {
      withSQL {
        select.from(AuthId as a)
          .where.append(sqls"${where}")
      }.map(AuthId(a)).list.apply
    }

  def findAllIdsByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[List[Long]] = Future {
    withSQL {
      select(column.id).from(AuthId as a)
        .where.eq(a.userId, userId)
    }.map(rs => rs.long(column.id)).list.apply
  }

  def create(id: Long, userId: Option[Int])(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[models.AuthId] = Future {
    withSQL {
      insert.into(AuthId).namedValues(
        column.id -> id,
        column.userId -> userId
      )
    }.execute.apply()
  } map (_ => models.AuthId(id, userId))

  def save(authId: models.AuthId)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[models.AuthId] = Future {
    withSQL {
      update(AuthId).set(
        column.userId -> authId.userId
      ).where.eq(column.id, authId.id)
    }.update.apply
    authId
  }

  def createOrUpdate(id: Long, userId: Option[Int])(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[models.AuthId] = find(id) flatMap { // TODO: don't select all rows, it's just a row presence check
    case Some(_) => save(models.AuthId(id, userId))
    case None => create(id, userId)
  }

  def destroy(id: Long)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[Boolean] = Future {
    withSQL {
      delete.from(AuthId).where.eq(column.id, id)
    }.execute.apply
  }
}
