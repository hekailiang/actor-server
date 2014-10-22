package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.secretapp.backend.crypto.ec.PublicKey
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data._
import com.secretapp.backend.data.models._
import com.websudos.phantom.Implicits._
import java.util.{ Date, UUID }
import org.joda.time.DateTime
import scala.collection.immutable
import scala.concurrent.Future
import scala.math.BigInt
import scodec.bits.BitVector

sealed class UserPublicKeyRecord extends CassandraTable[UserPublicKeyRecord, UserPublicKey] {
  override lazy val tableName = "user_public_keys"

  object uid extends IntColumn(this) with PartitionKey[Int]
  object publicKeyHash extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BlobColumn(this) {
    override lazy val name = "public_key"
  }
  object authId extends LongColumn(this) {
    override lazy val name = "auth_id"
  }
  object userAccessSalt extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "user_access_salt"
  }
  object deletedAt extends OptionalDateTimeColumn(this)

  override def fromRow(row: Row): UserPublicKey = {
    UserPublicKey(
      uid = uid(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row)),
      authId = authId(row),
      userAccessSalt = userAccessSalt(row)
    )
  }
}

object UserPublicKeyRecord extends UserPublicKeyRecord with DBConnector {
  def insertEntity(entity: UserPublicKey)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.uid, entity.uid)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.authId, entity.authId)
      .future()
  }

  def insertPartEntity(uid: Int, publicKeyHash: Long, publicKey: BitVector, authId: Long)(implicit session: Session): Future[ResultSet] = {
    insert.value(_.uid, uid)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, publicKey.toByteBuffer)
      .value(_.authId, authId)
      .future()
  }

  def getEntitiesByPublicKeyHash(uidAndPK: immutable.Seq[(Int, Long)])(implicit session: Session): Future[immutable.Seq[UserPublicKey]] = {
    val q = uidAndPK.map { t =>
      val (uid, pk) = t
      select.where(_.uid eqs uid).and(_.publicKeyHash eqs pk).one()
    }
    Future.sequence(q).map(_.filter(_.isDefined).map(_.get))
  }

  def getAuthIdByUidAndPublicKeyHash(uid: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[Long]] = {
    select(_.authId).where(_.uid eqs uid).and(_.publicKeyHash eqs publicKeyHash).one()
  }

  def fetchAuthIdsByUid(uid: Int)(implicit session: Session): Future[Seq[Long]] = {
    select(_.authId, _.deletedAt).where(_.uid eqs uid).fetch() map { pairs =>
      pairs filterNot (_._2.isDefined) map (_._1)
    }
  }

  def getAuthIdAndSalt(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[(Long, String)]] = {
    select(_.authId, _.userAccessSalt).where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash).one()
  }

  def setDeleted(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[ResultSet] = {
    update.where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash)
      .modify(_.deletedAt setTo Some(new DateTime)).future()
  }
}
