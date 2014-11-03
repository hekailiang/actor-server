package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import org.joda.time.DateTime
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scodec.bits.BitVector

sealed class UserPublicKey extends CassandraTable[UserPublicKey, models.UserPublicKey] {
  override val tableName = "user_public_keys"

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

  object deletedAt extends OptionalDateTimeColumn(this) {
    override lazy val name = "deleted_at"
  }

  object isDeleted extends BooleanColumn(this) with Index[Boolean] {
    override lazy val name = "is_deleted"
  }

  override def fromRow(row: Row): models.UserPublicKey =
    models.UserPublicKey(
      uid = uid(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row)),
      authId = authId(row),
      userAccessSalt = userAccessSalt(row)
    )
}

object UserPublicKey extends UserPublicKey with TableOps {
  import scalaz._
  import Scalaz._

  def insertEntity(entity: models.UserPublicKey)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.uid, entity.uid)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.authId, entity.authId)
      .value(_.isDeleted, false)
      .future()

  def insertDeletedEntity(entity: models.UserPublicKey)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.uid, entity.uid)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.authId, entity.authId)
      .value(_.isDeleted, true)
      .value(_.deletedAt, Some(new DateTime))
      .future()

  def insertEntityRow(uid: Int, publicKeyHash: Long, publicKey: BitVector, authId: Long)(implicit session: Session): Future[ResultSet] =
    insert.value(_.uid, uid)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, publicKey.toByteBuffer)
      .value(_.authId, authId)
      .value(_.isDeleted, false)
      .future()

  def getEntitiesByUserId(userId: Int)(implicit session: Session): Future[Seq[models.UserPublicKey]] =
    select.where(_.uid eqs userId).and(_.isDeleted eqs false).fetch()

  def getDeletedEntitiesByUserId(userId: Int)(implicit session: Session): Future[Seq[models.UserPublicKey]] =
    select.where(_.uid eqs userId).and(_.isDeleted eqs true).fetch()

  def getEntitiesByPublicKeyHash(uidAndPK: immutable.Seq[(Int, Long)])(implicit session: Session): Future[immutable.Seq[models.UserPublicKey]] = {
    val q = uidAndPK.map { t =>
      val (uid, pk) = t
      select.where(_.uid eqs uid).and(_.publicKeyHash eqs pk).one()
    }
    Future.sequence(q).map(_.filter(_.isDefined).map(_.get))
  }

  /**
    * Gets authId by userId and public key hash
    *
    * @param uid user id
    * @param publicKeyHash user public key hash
    * @return an authId value, right if its public key is active, left otherwise
    */
  def getAuthIdByUidAndPublicKeyHash(uid: Int, publicKeyHash: Long)
                                    (implicit session: Session): Future[Option[Long \/ Long]] =
    select(_.authId, _.isDeleted)
      .where(_.uid eqs uid).and(_.publicKeyHash eqs publicKeyHash)
      .one() map { optAuthId =>
        optAuthId map {
          case (authId, false) => authId.right
          case (authId, true)  => authId.left
        }
      }

  /**
    * Gets authIds and key hashes by userId, active only
    *
    * @param userId user id
    * @return Future of map of key hashes and auth ids
    */
  def fetchAuthIdsMap(uid: Int)(implicit session: Session): Future[Map[Long, Long]] =
    select(_.publicKeyHash, _.authId).where(_.uid eqs uid).and(_.isDeleted eqs false).fetch() map (_.toMap)

  /**
    * Gets authIds and key hashes by userId, including deleted
    *
    * @param userId user id
    * @return Future of map of key hashes and auth ids eithers
    */
  def fetchAllAuthIdsMap(uid: Int)(implicit session: Session): Future[Map[Long, Long \/ Long]] =
    select(_.publicKeyHash, _.authId, _.isDeleted).where(_.uid eqs uid).fetch() map { tuples =>
      tuples map {
        case (keyHash, authId, false) => (keyHash, authId.right)
        case (keyHash, authId, true) => (keyHash, authId.left)
      } toMap
    }

  def fetchAuthIdsByUserId(uid: Int)(implicit session: Session): Future[Seq[Long]] =
    select(_.authId).where(_.uid eqs uid).and(_.isDeleted eqs false).fetch()

  def getAuthIdAndSalt(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[(Long, String)]] =
    select(_.authId, _.userAccessSalt).where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash).one()

  def setDeleted(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[Long]] =
    select.where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash).one() flatMap {
      case Some(upk) => insertDeletedEntity(upk) map (_ => Some(upk.authId))
      case None => Future.successful(None)
    }
}
