package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.OptionT._

object UserEmail extends SQLSyntaxSupport[models.UserEmail] {
  override val tableName = "user_emails"
  override val columnNames = Seq("id", "email", "user_id", "title", "access_salt")

  lazy val ue = UserEmail.syntax("ue")

  def apply(a: SyntaxProvider[models.UserEmail])(rs: WrappedResultSet): models.UserEmail = apply(ue.resultName)(rs)

  def apply(u: ResultName[models.UserEmail])(rs: WrappedResultSet): models.UserEmail =
    models.UserEmail(
      id = rs.int(u.id),
      email = rs.string(u.email),
      userId = rs.int(u.userId),
      title = rs.string(u.title),
      accessSalt = rs.string(u.accessSalt)
    )

  def create(id: Int, email: String, title: String, accessSalt: String, userId: Int)
            (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Unit] = Future {
    blocking {
      withSQL {
        insert.into(UserEmail).namedValues(
          column.id -> id,
          column.email -> email,
          column.title -> title,
          column.accessSalt -> accessSalt,
          column.userId -> userId
        )
      }.execute().apply
    }
  }

  def findAllByUserId(userId: Int)
                     (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Seq[models.UserEmail]] =
    Future {
      blocking {
        withSQL {
          select.from(UserEmail as ue)
            .where.eq(ue.userId, userId)
        }.map(UserEmail(ue)).list().apply
      }
    }

  def findByEmail(email: String)
                 (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Option[models.UserEmail]] =
    Future {
      blocking {
        withSQL {
          select.from(UserEmail as ue)
            .where.eq(ue.email, email)
        }.map(UserEmail(ue)).single().apply
      }
    }


}
