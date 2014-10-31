package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.collection.immutable
import scala.concurrent.Future

sealed class PhoneRecord extends CassandraTable[PhoneRecord, models.Phone] {
  override lazy val tableName = "phones"

  object number extends LongColumn(this) with PartitionKey[Long]
  object userId extends IntColumn(this) with Index[Int] {
    override lazy val name = "user_id"
  }
  object userAccessSalt extends StringColumn(this) {
    override lazy val name = "user_access_salt"
  }

  object userName extends StringColumn(this) {
    override lazy val name = "user_first_name"
  }
  object userSex extends IntColumn(this){
    override lazy val name = "user_sex"
  }

  override def fromRow(row: Row): models.Phone =
    models.Phone(number = number(row), userId = userId(row), userAccessSalt = userAccessSalt(row),
      userName = userName(row), userSex = models.Sex.fromInt(userSex(row)))
}

object PhoneRecord extends PhoneRecord with DBConnector {
  def insertEntity(entity: models.Phone)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.number, entity.number)
      .value(_.userId, entity.userId)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.userName, entity.userName)
      .value(_.userSex, entity.userSex.toInt)
      .future()
  }

  def dropEntity(phoneNumber: Long)(implicit session: Session) = {
    delete.where(_.number eqs phoneNumber).future()
  }

  def updateUserName(phoneNumber: Long, userName: String)(implicit session: Session) = {
    update.
      where(_.number eqs phoneNumber).
      modify(_.userName setTo userName).
      future()
  }

  def updateUser(phoneNumber: Long, user: models.User)(implicit session: Session) = {
    update.
      where(_.number eqs phoneNumber).
      modify(_.userName setTo user.name).
      and(_.userSex setTo user.sex.toInt).
      future()
  }

  def getEntity(number: Long)(implicit session: Session): Future[Option[models.Phone]] = {
    select.where(_.number eqs number).one()
  }

  def getEntities(numbers: immutable.Set[Long])(implicit session: Session): Future[immutable.Set[models.Phone]] = {
    val q = numbers.map { number =>
      select.where(_.number eqs number).one()
    }
    Future.sequence(q).map(_.flatten)
  }
}
