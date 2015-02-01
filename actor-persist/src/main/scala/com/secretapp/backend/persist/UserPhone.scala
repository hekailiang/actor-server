package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.{ ExecutionContext, Future, blocking }
import scala.collection.immutable
import scalikejdbc._

object UserPhone extends SQLSyntaxSupport[models.UserPhone] {
  override val tableName = "user_phones"
  override val columnNames = Seq("user_id", "id", "access_salt", "number", "title")

  lazy val up = UserPhone.syntax("up")

  def apply(a: SyntaxProvider[models.UserPhone])(rs: WrappedResultSet): models.UserPhone = apply(up.resultName)(rs)

  def apply(a: ResultName[models.UserPhone])(rs: WrappedResultSet): models.UserPhone = models.UserPhone(
    id = rs.int(a.id),
    userId = rs.int(a.userId),
    accessSalt = rs.string(a.accessSalt),
    number = rs.long(a.number),
    title = rs.string(a.title)
  )

  def findBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[Option[models.UserPhone]] = Future {
    withSQL {
      select.from(UserPhone as up)
        .where.append(sqls"${where}")
        .limit(1)
    }.map(UserPhone(up)).single.apply
  }

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[List[models.UserPhone]] = Future {
    withSQL {
      select.from(UserPhone as up)
        .where.append(sqls"${where}")
    }.map(UserPhone(up)).list.apply
  }

  def findAllByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[List[models.UserPhone]] = findAllBy(sqls.eq(up.userId, userId))

  def findAllByNumbers(numbers: immutable.Set[Long])(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[List[models.UserPhone]] = findAllBy(sqls.in(up.number, numbers.toSeq))

  def findByNumber(number: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[Option[models.UserPhone]] = findBy(sqls.eq(column.number, number))

  def findByUserIdAndId(userId: Int, id: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[Option[models.UserPhone]] = findBy(
    sqls.eq(up.userId, userId)
      .and.eq(up.id, id)
  )

  def findFirstByUserId(userId: Int)
                       (implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession): Future[Option[models.UserPhone]] =
    findBy(sqls.eq(column.userId, userId))

  def create(userId: Int, id: Int, accessSalt: String, number: Long, title: String)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[models.UserPhone] = Future {
    withSQL {
      insert.into(UserPhone).namedValues(
        column.userId -> userId,
        column.id -> id,
        column.accessSalt -> accessSalt,
        column.number -> number,
        column.title -> title
      )
    }.execute.apply
    models.UserPhone(
      id = id,
      userId = userId,
      accessSalt = accessSalt,
      number = number,
      title = title
    )
  }

  def updateTitle(userId: Int, id: Int, title: String)(
    implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession
  ): Future[Int] = Future {
    withSQL {
      update(UserPhone).set(
        column.title -> title
      )
        .where.eq(column.userId, userId)
        .and.eq(column.id, id)
    }.update.apply
  }

  def getLatestNumbers(userIds: Seq[Int])
                      (implicit ec: ExecutionContext, session: DBSession = UserPhone.autoSession): Future[Seq[(Int, Long)]] =
    Future {
      blocking {
        sql"""
           select user_id, max(number) as number
           from $table
           where user_id in ($userIds)
           group by user_id
           """.map { rs => (rs.int("user_id"), rs.long("number")) }.list().apply()
      }
    }
}
