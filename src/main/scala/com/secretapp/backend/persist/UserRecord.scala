package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data._
import com.secretapp.backend.data.models._
import java.util.{ Date, UUID }
import scala.concurrent.Future
import scala.math.BigInt
import scalaz._
import Scalaz._

sealed class UserRecord extends CassandraTable[UserRecord, Entity[Int, User]] {
  override lazy val tableName = "users"

  object id extends IntColumn(this) with PartitionKey[Int]
  object accessHash extends LongColumn(this) {
    override lazy val name = "access_hash"
  }
  object firstName extends StringColumn(this) {
    override lazy val name = "first_name"
  }
  object lastName extends OptionalStringColumn(this) {
    override lazy val name = "last_name"
  }
  object sex extends OptionalIntColumn(this)
//  object keyHashes extends OptionalIntColumn(this)

  override def fromRow(row: Row): Entity[Int, User] = {
    Entity(id(row), User(accessHash(row), firstName(row), lastName(row), sex(row).flatMap(intToSex(_).some)))
  }
}

object UserRecord extends UserRecord with DBConnector {
  def insertEntity(entity: Entity[Int, User])(implicit session: Session): Future[ResultSet] = entity match {
    case Entity(id, user) =>
      insert.value(_.id, id)
        .value(_.accessHash, user.accessHash)
        .value(_.firstName, user.firstName)
        .value(_.lastName, user.lastName)
        .value(_.sex, user.sex.flatMap(sexToInt(_).some))
        .future()
  }

  def getEntity(id: Int)(implicit session: Session): Future[Option[Entity[Int, User]]] = {
    select.where(_.id eqs id).one()
  }
}
