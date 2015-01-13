package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.Future
import scalikejdbc._, async._, FutureImplicits._

object UserPhone extends SQLSyntaxSupport[models.UserPhone] with ShortenedNames {
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
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.UserPhone]] = withSQL {
    select.from(UserPhone as up)
      .where.append(sqls"${where}")
      .limit(1)
  } map (UserPhone(up))

  def findAllBy(where: SQLSyntax)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[models.UserPhone]] = withSQL {
    select.from(UserPhone as up)
      .where.append(sqls"${where}")
  } map (UserPhone(up))

  def findAllByUserId(userId: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[models.UserPhone]] = findAllBy(sqls.eq(up.userId, userId))

  def findByUserIdAndId(userId: Int, id: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.UserPhone]] = findBy(
    sqls.eq(up.userId, userId)
      .and.eq(up.id, id)
  )

  def create(userId: Int, id: Int, accessSalt: String, number: Long, title: String)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.UserPhone] = for {
    _ <- withSQL {
      insert.into(UserPhone).namedValues(
        column.userId -> userId,
        column.id -> id,
        column.accessSalt -> accessSalt,
        column.number -> number,
        column.title -> title
      )
    }.execute.future
  } yield models.UserPhone(
    id = id,
    userId = userId,
    accessSalt = accessSalt,
    number = number,
    title = title
  )

  def updateTitle(userId: Int, id: Int, title: String)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Int] = withSQL {
    update(UserPhone).set(
      column.title -> title
    )
      .where.eq(column.userId, userId)
      .and.eq(column.id, id)
  }.update.future
}
