package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import java.nio.ByteBuffer
import org.joda.time.DateTime
import scala.concurrent.Future
import scodec.bits._

trait AbstractAuthSession[T <: CassandraTable[T, R], R] extends CassandraTable[T, R] {
  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }

  object id extends IntColumn(this) with PrimaryKey[Int]

  object appId extends IntColumn(this) {
    override lazy val name = "app_id"
  }

  object appTitle extends StringColumn(this) {
    override lazy val name = "app_title"
  }

  object deviceTitle extends StringColumn(this) {
    override lazy val name = "device_title"
  }

  object authTime extends IntColumn(this) {
    override lazy val name = "auth_time"
  }

  object authLocation extends StringColumn(this) {
    override lazy val name = "auth_location"
  }

  object latitude extends OptionalDoubleColumn(this)

  object longitude extends OptionalDoubleColumn(this)
}

sealed class AuthSession extends AbstractAuthSession[AuthSession, models.AuthSession] {
  override val tableName = "auth_sessions"

  object deviceHash extends BlobColumn(this) with Index[ByteBuffer] {
    override lazy val name = "device_hash"
  }

  object authId extends LongColumn(this) {
    override lazy val name = "auth_id"
  }

  object publicKeyHash extends LongColumn(this) with Index[Long] {
    override lazy val name = "public_key_hash"
  }

  override def fromRow(row: Row): models.AuthSession =
    models.AuthSession(
      id(row), appId(row), appTitle(row), deviceTitle(row), authTime(row),
      authLocation(row), latitude(row), longitude(row),
      authId(row), publicKeyHash(row), BitVector(deviceHash(row))
    )
}

sealed class DeletedAuthSession extends AbstractAuthSession[DeletedAuthSession, models.AuthSession] {
  override val tableName = "deleted_auth_sessions"

  object deviceHash extends BlobColumn(this) with Index[ByteBuffer] {
    override lazy val name = "device_hash"
  }

  object authId extends LongColumn(this) {
    override lazy val name = "auth_id"
  }

  object publicKeyHash extends LongColumn(this) with Index[Long] {
    override lazy val name = "public_key_hash"
  }

  object deletedAt extends DateTimeColumn(this) {
    override lazy val name = "deleted_at"
  }

  override def fromRow(row: Row): models.AuthSession =
    models.AuthSession(
      id(row), appId(row), appTitle(row), deviceTitle(row), authTime(row),
      authLocation(row), latitude(row), longitude(row),
      authId(row), publicKeyHash(row), BitVector(deviceHash(row))
    )
}

object DeletedAuthSession extends DeletedAuthSession with TableOps {
  private def insertQuery(item: models.AuthSession, userId: Int) = {
    insert.value(_.userId, userId).value(_.id, item.id)
      .value(_.deviceHash, item.deviceHash.toByteBuffer)
      .value(_.publicKeyHash, item.publicKeyHash)
      .value(_.authId, item.authId)
      .value(_.appId, item.appId).value(_.appTitle, item.appTitle)
      .value(_.deviceTitle, item.deviceTitle).value(_.authTime, item.authTime)
      .value(_.authLocation, item.authLocation).value(_.latitude, item.latitude).value(_.longitude, item.longitude)
  }

  def insertEntity(item: models.AuthSession, userId: Int)(implicit session: Session): Future[ResultSet] = {
    insertQuery(item, userId).value(_.deletedAt, new DateTime).future()
  }

  def getEntitiesByUserId(userId: Int)(implicit session: Session): Future[Seq[models.AuthSession]] = {
    select.where(_.userId eqs userId).fetch()
  }
}

object AuthSession extends AuthSession with TableOps {
  private def insertQuery(item: models.AuthSession, userId: Int) = {
    insert.value(_.userId, userId).value(_.id, item.id)
      .value(_.deviceHash, item.deviceHash.toByteBuffer)
      .value(_.publicKeyHash, item.publicKeyHash)
      .value(_.authId, item.authId)
      .value(_.appId, item.appId).value(_.appTitle, item.appTitle)
      .value(_.deviceTitle, item.deviceTitle).value(_.authTime, item.authTime)
      .value(_.authLocation, item.authLocation).value(_.latitude, item.latitude).value(_.longitude, item.longitude)
  }

  def insertEntity(item: models.AuthSession, userId: Int)(implicit session: Session): Future[ResultSet] = {
    AuthSession.insertQuery(item, userId).future()
  }

  def getEntity(userId: Int, id: Int)(implicit session: Session): Future[Option[models.AuthSession]] =
    select.where(_.userId eqs userId).and(_.id eqs id).one()

  def getEntityByUserIdAndPublicKeyHash(userId: Int, publicKeyHash: Long)(implicit session: Session): Future[Option[models.AuthSession]] = {
    select.where(_.userId eqs userId).and(_.publicKeyHash eqs publicKeyHash).one()
  }

  def getEntitiesByUserId(userId: Int)(implicit session: Session): Future[Seq[models.AuthSession]] = {
    select.where(_.userId eqs userId).fetch()
  }

  def getEntitiesByUserIdAndDeviceHash(userId: Int, deviceHash: BitVector)(implicit session: Session): Future[Seq[models.AuthSession]] = {
    select.where(_.userId eqs userId).and(_.deviceHash eqs deviceHash.toByteBuffer).fetch()
  }

  def getEntities(userId: Int)(implicit session: Session): Future[Seq[models.AuthSession]] =
    select.where(_.userId eqs userId).fetch()

  def setDeleted(userId: Int, id: Int)(implicit session: Session): Future[Option[Long]] = {
    select.where(_.userId eqs userId).and(_.id eqs id).one() flatMap {
      case Some(authItem) =>
        DeletedAuthSession.insertEntity(authItem, userId) flatMap { _ =>
          delete.where(_.userId eqs userId).and(_.id eqs id).future() map (_ => Some(authItem.authId))
        }
      case None =>
        Future.successful(None)
    }
  }
}
