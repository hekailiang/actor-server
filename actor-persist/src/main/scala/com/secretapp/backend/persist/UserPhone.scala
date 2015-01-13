package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Failure, Success }

sealed class UserPhone extends CassandraTable[UserPhone, models.UserPhone] {
  override val tableName = "user_phones"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override val name = "user_id"
  }

  object phoneId extends IntColumn(this) with PrimaryKey[Int] {
    override val name = "phone_id"
  }

  object accessSalt extends StringColumn(this) {
    override val name = "access_salt"
  }

  object number extends LongColumn(this)

  object title extends StringColumn(this)

  override def fromRow(row: Row): models.UserPhone =
    models.UserPhone(
      id = phoneId(row),
      userId = userId(row),
      accessSalt = accessSalt(row),
      number = number(row),
      title = title(row)
    )
}

object UserPhone extends UserPhone with TableOps {
  def insertEntity(entity: models.UserPhone)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.userId, entity.userId)
      .value(_.phoneId, entity.id)
      .value(_.accessSalt, entity.accessSalt)
      .value(_.number, entity.number)
      .value(_.title, entity.title)
      .future()

  def getEntity(userId: Int, phoneId: Int)(implicit session: Session): Future[Option[models.UserPhone]] =
    select.where(_.userId eqs userId).and(_.phoneId eqs phoneId).one()

  def fetchUserPhones(userId: Int)(implicit session: Session): Future[Seq[models.UserPhone]] =
    select.where(_.userId eqs userId).fetch()

  def fetchUserPhoneIds(userId: Int)(implicit session: Session): Future[Seq[Int]] =
    select(_.phoneId).where(_.userId eqs userId).fetch()

  def editTitle(userId: Int, phoneId: Int, title: String)(implicit session: Session): Future[ResultSet] =
    update
      .where(_.userId eqs userId).and(_.phoneId eqs phoneId)
      .modify(_.title setTo title).future()
}
