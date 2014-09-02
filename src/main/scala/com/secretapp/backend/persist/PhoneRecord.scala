package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import scala.collection.immutable
import scala.concurrent.Future

sealed class PhoneRecord extends CassandraTable[PhoneRecord, Phone] {
  override lazy val tableName = "phones"

  object number extends LongColumn(this) with PartitionKey[Long]
  object userId extends IntColumn(this) {
    override lazy val name = "user_id"
  }
  object userAccessSalt extends StringColumn(this) {
    override lazy val name = "user_access_salt"
  }
  object userKeyHashes extends SetColumn[PhoneRecord, Phone, Long](this) {
    override lazy val name = "user_key_hashes"
  }
  object userFirstName extends StringColumn(this) {
    override lazy val name = "user_first_name"
  }
  object userLastName extends OptionalStringColumn(this) {
    override lazy val name = "user_last_name"
  }
  object userSex extends IntColumn(this){
    override lazy val name = "user_sex"
  }

  override def fromRow(row: Row): Phone = {
    Phone(number = number(row), userId = userId(row), userAccessSalt = userAccessSalt(row),
      userFirstName = userFirstName(row), userLastName = userLastName(row),
      userKeyHashes = userKeyHashes(row), userSex = intToSex(userSex(row)))
  }
}

object PhoneRecord extends PhoneRecord with DBConnector {
  def insertEntity(entity: Phone)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.number, entity.number)
      .value(_.userId, entity.userId)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.userKeyHashes, entity.userKeyHashes.toSet)
      .value(_.userFirstName, entity.userFirstName)
      .value(_.userLastName, entity.userLastName)
      .value(_.userSex, sexToInt(entity.userSex))
      .future()
  }

  def dropEntity(phoneNumber: Long)(implicit session: Session) = {
    delete.where(_.number eqs phoneNumber).future()
  }

  def addKeyHash(phoneNumber: Long, keyHash: Long)(implicit session: Session) = {
    update.where(_.number eqs phoneNumber).modify(_.userKeyHashes add keyHash).future()
  }

  def removeKeyHash(phoneNumber: Long, keyHash: Long)(implicit session: Session) = {
    update.where(_.number eqs phoneNumber).modify(_.userKeyHashes remove keyHash).future()
  }

  def updateUser(phoneNumber: Long, user: User)(implicit session: Session) = {
    update.
      where(_.number eqs phoneNumber).
      modify(_.userFirstName setTo user.firstName).
      and(_.userLastName setTo user.lastName).
      and(_.userSex setTo sexToInt(user.sex)).
      future()
  }

  def getEntity(number: Long)(implicit session: Session): Future[Option[Phone]] = {
    select.where(_.number eqs number).one()
  }

  def getEntities(numbers: immutable.Seq[Long])(implicit session: Session): Future[immutable.Seq[Phone]] = {
    val q = numbers.map { number =>
      select.where(_.number eqs number).one()
    }
    Future.sequence(q).map(_.filter(_.isDefined).map(_.get))
  }
}
