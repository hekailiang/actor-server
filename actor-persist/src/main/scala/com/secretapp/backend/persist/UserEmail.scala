package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.collection.immutable
import scala.concurrent.Future

sealed class UserEmail extends CassandraTable[UserEmail, models.UserEmail] {
  override val tableName = "user_emails"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override val name = "user_id"
  }

  object emailId extends IntColumn(this) with PrimaryKey[Int] {
    override val name = "email_id"
  }

  object accessSalt extends StringColumn(this) {
    override val name = "access_salt"
  }

  object email extends StringColumn(this)

  object title extends StringColumn(this)

  override def fromRow(row: Row): models.UserEmail =
    models.UserEmail(
      id = emailId(row),
      userId = userId(row),
      accessSalt = accessSalt(row),
      email = email(row),
      title = title(row)
    )
}

object UserEmail extends UserEmail with TableOps {
  def insertEntity(entity: models.UserEmail)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.userId, entity.userId)
      .value(_.emailId, entity.id)
      .value(_.accessSalt, entity.accessSalt)
      .value(_.email, entity.email)
      .value(_.title, entity.title)
      .future()

  def getEntity(userId: Int, emailId: Int)(implicit session: Session): Future[Option[models.UserEmail]] =
    select.where(_.userId eqs userId).and(_.emailId eqs emailId).one()

  def fetchUserEmails(userId: Int)(implicit session: Session): Future[Seq[models.UserEmail]] =
    select.where(_.userId eqs userId).fetch()
}
