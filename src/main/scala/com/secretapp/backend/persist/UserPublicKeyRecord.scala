package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data._
import com.secretapp.backend.data.models._
import com.secretapp.backend.crypto.ec.PublicKey
import java.util.{ Date, UUID }
import scodec.bits.BitVector
import scala.concurrent.Future
import scala.math.BigInt
import scala.collection.immutable
import scalaz._
import Scalaz._

sealed class UserPublicKeyRecord extends CassandraTable[UserPublicKeyRecord, UserPublicKey] {
  override lazy val tableName = "user_public_keys"

  object uid extends IntColumn(this) with PartitionKey[Int]
  object publicKeyHash extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BigIntColumn(this) {
    override lazy val name = "public_key"
  }
  object userAccessSalt extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "user_access_salt"
  }

  override def fromRow(row: Row): UserPublicKey = {
    UserPublicKey(
      uid = uid(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row).toByteArray),
      userAccessSalt = userAccessSalt(row)
    )
  }
}

object UserPublicKeyRecord extends UserPublicKeyRecord with DBConnector {
  def insertEntity(entity: UserPublicKey)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.uid, entity.uid)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, BigInt(entity.publicKey.toByteArray))
      .value(_.userAccessSalt, entity.userAccessSalt)
      .future()
  }

  def insertPartEntity(uid: Int, publicKeyHash: Long, publicKey: BitVector)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.uid, uid)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, BigInt(publicKey.toByteArray))
      .future()
  }

  def getEntitiesByPublicKeyHash(uidAndPK: immutable.Seq[(Int, Long)])(implicit session: Session): Future[immutable.Seq[UserPublicKey]] = {
    val q = uidAndPK.map { t =>
      val (uid, pk) = t
      select.where(_.uid eqs uid).and(_.publicKeyHash eqs pk).one()
    }
    Future.sequence(q).map(_.filter(_.isDefined).map(_.get))
  }
}
