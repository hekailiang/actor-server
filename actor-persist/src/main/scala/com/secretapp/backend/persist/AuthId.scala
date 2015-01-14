package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._

object AuthId extends SQLSyntaxSupport[models.AuthId] {
  override val tableName = "auth_ids"
  override val columnNames = Seq("id", "user_id", "deleted_at")

  lazy val a = AuthId.syntax("a")
  private val isNotDeleted = sqls.isNull(a.column("deleted_at"))
  private val isDeleted = sqls.isNotNull(a.column("deleted_at"))

  def apply(a: SyntaxProvider[models.AuthId])(rs: WrappedResultSet): models.AuthId = apply(a.resultName)(rs)

  def apply(a: ResultName[models.AuthId])(rs: WrappedResultSet): models.AuthId = models.AuthId(
    id = rs.long(a.id),
    userId = rs.intOpt(a.userId)
  )

  def findBySync(where: SQLSyntax)(
    implicit session: DBSession
  ): Option[models.AuthId] = withSQL {
    select.from(AuthId as a)
      .where.append(isNotDeleted).and.append(sqls"${where}")
      .limit(1)
  }.map(AuthId(a)).single.apply()

  def findBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[Option[models.AuthId]] =
    Future {
      blocking {
        findBySync(where)
      }
    }

  def findAllBySync(where: SQLSyntax)(
    implicit session: DBSession
  ): List[models.AuthId] = withSQL {
    select.from(AuthId as a)
      .where.append(isNotDeleted).and.append(sqls"${where}")
  }.map(AuthId(a)).list.apply()

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[List[models.AuthId]] =
    Future {
      blocking {
        findAllBySync(where)
      }
    }

  def find(id: Long)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[Option[models.AuthId]] = findBy(sqls.eq(a.id, id))

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
    blocking {
      withSQL {
        select(column.id).from(AuthId as a)
          .where.append(isNotDeleted)
          .and.eq(a.userId, userId).limit(1)
      }.map(rs => rs.long(column.id)).single.apply
    }
  }

  def findAllIdsByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[List[Long]] = Future {
    blocking {
      withSQL {
        select(column.id).from(AuthId as a)
          .where.append(isNotDeleted)
          .and.eq(a.userId, userId)
      }.map(rs => rs.long(column.id)).list.apply
    }
  }

  def create(id: Long, userId: Option[Int])(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[models.AuthId] = Future {
    blocking {
      withSQL {
        insert.into(AuthId).namedValues(
          column.id -> id,
          column.userId -> userId
        )
      }.execute.apply()
    }
  } map (_ => models.AuthId(id, userId))

  def save(authId: models.AuthId)(
    implicit ec: ExecutionContext, session: DBSession = AuthId.autoSession
  ): Future[models.AuthId] = Future {
    blocking {
      withSQL {
        update(AuthId).set(
          column.userId -> authId.userId
        ).where.eq(column.id, authId.id)
      }.update.apply
    }

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
  ): Future[Int] = Future {
    blocking {
      destroySync(id)
    }
  }

  def destroySync(id: Long): Int = DB localTx { implicit s =>
    val pkOpt = UserPublicKey.findByAuthIdSync(id)

    pkOpt map (pk => UserPublicKey.destroySync(pk.userId, pk.hash))

    withSQL {
      update(AuthId).set(column.column("deleted_at") -> DateTime.now)
        .where.eq(column.id, id)
    }.update.apply()
  }
}
