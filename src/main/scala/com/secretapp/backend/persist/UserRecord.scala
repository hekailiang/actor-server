package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data._
import java.util.{ Date, UUID }
import scala.concurrent.Future
import scala.math.BigInt

sealed class UserRecord extends CassandraTable[UserRecord, Entity[Long, User]] {
  override lazy val tableName = "users"

  object id extends BigIntColumn(this) with PartitionKey[BigInt]
  object firstName extends StringColumn(this) {
    override lazy val name = "first_name"
  }
  object lastName extends StringColumn(this) {
    override lazy val name = "last_name"
  }
  object sex extends IntColumn(this)

  override def fromRow(row: Row): Entity[Long, User] = {
    Entity(id(row).toLong, User(firstName(row), lastName(row), sex(row)))
  }
}

object UserRecord extends UserRecord with DBConnector {
  def insertEntity(entity: Entity[Long, User]): Future[ResultSet] = entity match {
    case Entity(id, user) =>
      insert.value(_.id, BigInt.long2bigInt(id))
        .value(_.firstName, user.firstName)
        .value(_.lastName, user.lastName)
        .value(_.sex, SexToInt(user.sex))
        .future()
  }

  def getEntity(id: Long): Future[Option[Entity[Long, User]]] = {
    select.where(_.id eqs id).one()
  }
}
