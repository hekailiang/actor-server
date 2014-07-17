package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data._
import com.secretapp.backend.data.models._
import java.util.{ Date, UUID }
import scodec.bits.BitVector
import scala.concurrent.Future
import scala.math.BigInt
import scalaz._
import Scalaz._

sealed class UserRecord extends CassandraTable[UserRecord, Entity[Int, User]] {
  override lazy val tableName = "users"

  object id extends IntColumn(this) with PartitionKey[Int]
  object publicKeyHash extends LongColumn(this) {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BigIntColumn(this) {
    override lazy val name = "public_key"
  }
  object accessSalt extends StringColumn(this) {
    override lazy val name = "access_salt"
  }
  object firstName extends StringColumn(this) {
    override lazy val name = "first_name"
  }
  object lastName extends OptionalStringColumn(this) {
    override lazy val name = "last_name"
  }
  object sex extends IntColumn(this)

  override def fromRow(row: Row): Entity[Int, User] = {
    Entity(id(row), User(publicKeyHash(row), BitVector(publicKey(row).toByteArray), accessSalt(row), firstName(row),
      lastName(row), intToSex(sex(row)) ))
  }
}

object UserRecord extends UserRecord with DBConnector {
  def insertEntity(entity: Entity[Int, User])(implicit session: Session): Future[ResultSet] = entity match {
    case Entity(id, user) =>
      insert.value(_.id, id)
        .value(_.publicKeyHash, user.publicKeyHash)
        .value(_.publicKey, BigInt(user.publicKey.toByteArray))
        .value(_.accessSalt, user.accessSalt)
        .value(_.firstName, user.firstName)
        .value(_.lastName, user.lastName)
        .value(_.sex, sexToInt(user.sex))
        .future()
  }

  def getEntity(id: Int)(implicit session: Session): Future[Option[Entity[Int, User]]] = {
    select.where(_.id eqs id).one()
  }
}
