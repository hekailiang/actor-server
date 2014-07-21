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

sealed class UserRecord extends CassandraTable[UserRecord, User] {
  override lazy val tableName = "users"

  object id extends IntColumn(this) with PartitionKey[Int]
  object publicKeyHash extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BigIntColumn(this) {
    override lazy val name = "public_key"
  }
  object keyHashes extends SetColumn[UserRecord, User, Long](this) with StaticColumn[Set[Long]] {
    override lazy val name = "key_hashes"
  }
  object accessSalt extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "access_salt"
  }
  object firstName extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "first_name"
  }
  object lastName extends OptionalStringColumn(this) with StaticColumn[Option[String]] {
    override lazy val name = "last_name"
  }
  object sex extends IntColumn(this) with StaticColumn[Int]

  override def fromRow(row: Row): User = {
    User(
      id = id(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row).toByteArray),
      keyHashes = keyHashes(row).toIndexedSeq,
      accessSalt = accessSalt(row),
      firstName = firstName(row),
      lastName = lastName(row),
      sex = intToSex(sex(row))
    )
  }
}

object UserRecord extends UserRecord with DBConnector {
  def insertEntity(entity: User)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.id, entity.id)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, BigInt(entity.publicKey.toByteArray))
      .value(_.keyHashes, Set(entity.publicKeyHash))
      .value(_.accessSalt, entity.accessSalt)
      .value(_.firstName, entity.firstName)
      .value(_.lastName, entity.lastName)
      .value(_.sex, sexToInt(entity.sex))
      .future()
  }

  def insertPartEntity(entity: User)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.id, entity.id)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, BigInt(entity.publicKey.toByteArray))
      .future().flatMap(_ => addKeyHash(entity))
  }

  private def addKeyHash(user: User)(implicit session: Session) = {
    update.where(_.id eqs user.id).modify(_.keyHashes add user.publicKeyHash).future()
  }

  def getEntity(id: Int)(implicit session: Session): Future[Option[User]] = {
    select.where(_.id eqs id).one()
  }
}
