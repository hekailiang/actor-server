package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import org.joda.time.DateTime
import scala.collection.immutable
import scala.language.postfixOps
import scodec.bits.BitVector

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.query.SelectQuery
import play.api.libs.iteratee._
import scala.concurrent._, duration._
import scala.util
import scalikejdbc._

sealed class UserPublicKey extends CassandraTable[UserPublicKey, models.UserPublicKey] {
  override val tableName = "user_public_keys"

  object uid extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
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
      userId = uid(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row)),
      authId = authId(row),
      userAccessSalt = userAccessSalt(row)
    )

  def fromRowWithDeletedAt(row: Row): (models.UserPublicKey, Option[DateTime]) = {
    (
      fromRow(row),
      deletedAt(row)
    )
  }
}

object UserPublicKey extends UserPublicKey with TableOps {
  import scalaz._
  import Scalaz._

  def insertEntity(entity: models.UserPublicKey)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.uid, entity.userId)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.authId, entity.authId)
      .value(_.isDeleted, false)
      .future()

  def insertDeletedEntity(entity: models.UserPublicKey)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.uid, entity.userId)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.userAccessSalt, entity.userAccessSalt)
      .value(_.authId, entity.authId)
      .value(_.isDeleted, true)
      .value(_.deletedAt, Some(new DateTime))
      .future()

  def insertEntityRow(userId: Int, publicKeyHash: Long, publicKey: BitVector, authId: Long)(implicit session: Session): Future[ResultSet] =
    insert.value(_.uid, userId)
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
    * @param userId user id
    * @param publicKeyHash user public key hash
    * @return an authId value, right if its public key is active, left otherwise
    */
  def getAuthIdByUidAndPublicKeyHash(userId: Int, publicKeyHash: Long)
                                    (implicit session: Session): Future[Option[Long \/ Long]] =
    select(_.authId, _.isDeleted)
      .where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash)
      .one() map { optAuthId =>
        optAuthId map {
          case (authId, false) => authId.right
          case (authId, true)  => authId.left
        }
      }

  def fetchAuthIdsOfDeletedKeys(userId: Int, keyHashes: immutable.Set[Long])(implicit session: Session): Future[Seq[(Long, Long)]] = {
      select(_.publicKeyHash, _.authId)
        .where(_.uid eqs userId)
        .and(_.isDeleted eqs true)
        .and(_.publicKeyHash in keyHashes.toList).fetch()
  }

  def fetchAuthIdsOfActiveKeys(userId: Int)(implicit session: Session): Future[Seq[(Long, Long)]] = {
    select(_.publicKeyHash, _.authId)
      .where(_.uid eqs userId)
      .and(_.isDeleted eqs false)
      .fetch()
  }

  def fetchAuthIdsByUserId(userId: Int)(implicit session: Session): Future[Seq[Long]] =
    select(_.authId).where(_.uid eqs userId).and(_.isDeleted eqs false).fetch()

  def getAuthIdAndSalt(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[(Long, String)]] =
    select(_.authId, _.userAccessSalt).where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash).one()

  def setDeleted(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[Long]] =
    select.where(_.uid eqs userId).and(_.publicKeyHash eqs publicKeyHash).one() flatMap {
      case Some(upk) => insertDeletedEntity(upk) map (_ => Some(upk.authId))
      case None => Future.successful(None)
    }

  def main(args: Array[String]) {
    implicit val session = DBConnector.session
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    //DBConnector.flyway.migrate()
    println("migrated")

    val fails = moveToSQL()

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }

  def moveToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee: Iteratee[(models.UserPublicKey, Option[DateTime]), List[util.Try[Boolean]]] =
      Iteratee.fold[(models.UserPublicKey, Option[DateTime]), List[util.Try[Boolean]]](List.empty) {
        case (moves, (pk, deletedAt)) =>

        moves :+ util.Try {
          sql"""insert into public_keys (user_id, hash, data, auth_id, deleted_at)
                VALUES (${pk.userId}, ${pk.publicKeyHash}, ${pk.publicKey.toByteArray}, ${pk.authId}, ${deletedAt})"""
            .execute.apply
        }
      }

    val query = new SelectQuery[UserPublicKey, (models.UserPublicKey, Option[DateTime])](
      this.asInstanceOf[UserPublicKey],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[UserPublicKey].fromRowWithDeletedAt
    )

    val tries = Await.result(query.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case util.Failure(e) =>
        Some(e)
      case util.Success(_) =>
        None
    } flatten
  }
}
