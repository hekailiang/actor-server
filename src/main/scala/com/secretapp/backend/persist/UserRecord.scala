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
  object publicKeyHash extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BigIntColumn(this) {
    override lazy val name = "public_key"
  }
  object keyHashes extends SetColumn[UserRecord, Entity[Int, User], Long](this) with StaticColumn[Set[Long]] {
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

  override def fromRow(row: Row): Entity[Int, User] = {
    val user = User(
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row).toByteArray),
      keyHashes = keyHashes(row).toIndexedSeq,
      accessSalt = accessSalt(row),
      firstName = firstName(row),
      lastName = lastName(row),
      sex = intToSex(sex(row)) )
    Entity(id(row), user)
  }
}

object UserRecord extends UserRecord with DBConnector {
  def insertEntity(entity: Entity[Int, User])(implicit session: Session): Future[ResultSet] = entity match {
    case Entity(id, user) =>
      insert.value(_.id, id)
        .value(_.publicKeyHash, user.publicKeyHash)
        .value(_.publicKey, BigInt(user.publicKey.toByteArray))
        .value(_.keyHashes, Set(user.publicKeyHash))
        .value(_.accessSalt, user.accessSalt)
        .value(_.firstName, user.firstName)
        .value(_.lastName, user.lastName)
        .value(_.sex, sexToInt(user.sex))
        .future()
  }

  def insertPartEntity(entity: Entity[Int, User])(implicit session: Session): Future[ResultSet] = entity match {
    case Entity(id, user) =>
      insert.value(_.id, id)
        .value(_.publicKeyHash, user.publicKeyHash)
        .value(_.publicKey, BigInt(user.publicKey.toByteArray))
        .future().flatMap(_ => addKeyHash(id, user))
  }

  private def addKeyHash(id: Int, user: User)(implicit session: Session) = {
    update.where(_.id eqs id).modify(_.keyHashes add user.publicKeyHash).future()
  }

  def getEntity(id: Int)(implicit session: Session): Future[Option[Entity[Int, User]]] = {
    select.where(_.id eqs id).one()
  }
}
