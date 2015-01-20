package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.{ExecutionContext, Future}
import scalikejdbc._
import com.datastax.driver.core.{ Session => CSession }
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
      id = rs.int(u.column("id")),
      email = rs.string(u.email),
      userId = rs.int(u.userId),
      title = rs.string(u.title),
      accessSalt = rs.string(u.accessSalt)
    )

  def create(id: Int, email: String, title: String, accessSalt: String, userId: Int)
            (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Unit] = Future {
    withSQL {
      insert.into(UserEmail).namedValues(
        column.column("id") -> id,
        column.email -> email,
        column.title -> title,
        column.accessSalt -> accessSalt,
        column.userId -> userId
      )
    }.execute().apply
  }

  def findAllByUserId(userId: Int)
                     (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Seq[models.UserEmail]] =
    Future {
      withSQL {
        select.from(UserEmail as ue)
          .where.eq(ue.column("user_id"), userId)
      }.map(UserEmail(ue)).list().apply
    }

  def findByEmail(email: String)
                 (implicit ec: ExecutionContext, session: DBSession = UserEmail.autoSession): Future[Option[models.UserEmail]] =
    Future {
      withSQL {
        select.from(UserEmail as ue)
          .where.eq(ue.column("email"), email)
      }.map(UserEmail(ue)).single().apply
    }


  def getUser(email: String)
             (implicit ec: ExecutionContext, csession: CSession, session: DBSession = UserEmail.autoSession): Future[Option[models.User]] =
    {
      for {
         userEmail <- optionT(findByEmail(email))
         user <- optionT(User.find(userEmail.userId)(None))
      } yield user
    }.run
}
