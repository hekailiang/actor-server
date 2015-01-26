package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.query.SelectQuery
import java.nio.ByteBuffer
import org.joda.time.DateTime
import scala.concurrent.Future
import scodec.bits._

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }
import scalikejdbc._

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

  def fromRowWithUserId(row: Row): (models.AuthSession, Int) =
    (
      models.AuthSession(
        id(row), appId(row), appTitle(row), deviceTitle(row), authTime(row),
        authLocation(row), latitude(row), longitude(row),
        authId(row), publicKeyHash(row), BitVector(deviceHash(row))
      ),
      userId(row)
    )

  def selectWithUserId: SelectQuery[AuthSession, (models.AuthSession, Int)] =
    new SelectQuery[AuthSession, (models.AuthSession, Int)](
      this.asInstanceOf[AuthSession],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[AuthSession].fromRowWithUserId
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

  def fromRowWithUserIdAndDeletedAt(row: Row): (models.AuthSession, Int, DateTime) =
    (
      models.AuthSession(
        id(row), appId(row), appTitle(row), deviceTitle(row), authTime(row),
        authLocation(row), latitude(row), longitude(row),
        authId(row), publicKeyHash(row), BitVector(deviceHash(row))
      ),
      userId(row),
      deletedAt(row)
    )

  def selectWithUserIdAndDeletedAt: SelectQuery[DeletedAuthSession, (models.AuthSession, Int, DateTime)] =
    new SelectQuery[DeletedAuthSession, (models.AuthSession, Int, DateTime)](
      this.asInstanceOf[DeletedAuthSession],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[DeletedAuthSession].fromRowWithUserIdAndDeletedAt
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
    val moveIteratee =
      Iteratee.fold[(models.AuthSession, Int, DateTime), List[Try[Boolean]]](List.empty) {
        case (moves, (as, userId, deletedAt)) =>

          moves :+ Try {
            sql"""insert into auth_sessions (user_id, id, app_id, app_title, auth_id, public_key_hash, device_hash, device_title, auth_time, auth_location, deleted_at)
                VALUES (${userId}, ${as.id}, ${as.appId}, ${as.appTitle}, ${as.authId}, ${as.publicKeyHash}, ${as.deviceHash.toByteArray}, ${as.deviceTitle}, ${new DateTime(as.authTime.toLong * 1000)}, ${as.authLocation}, ${deletedAt})"""
              .execute.apply
          }
      }

    val tries = Await.result(selectWithUserIdAndDeletedAt.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case Failure(e) =>
        Some(e)
      case Success(_) =>
        None
    } flatten
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
    val moveIteratee =
      Iteratee.fold[(models.AuthSession, Int), List[Try[Boolean]]](List.empty) {
      case (moves, (as, userId)) =>

        moves :+ Try {
          sql"""insert into auth_sessions (user_id, id, app_id, app_title, auth_id, public_key_hash, device_hash, device_title, auth_time, auth_location)
                VALUES (${userId}, ${as.id}, ${as.appId}, ${as.appTitle}, ${as.authId}, ${as.publicKeyHash}, ${as.deviceHash.toByteArray}, ${as.deviceTitle}, ${new DateTime(as.authTime.toLong * 1000)}, ${as.authLocation})"""
            .execute.apply
        }
      }

    val tries = Await.result(selectWithUserId.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case Failure(e) =>
        Some(e)
      case Success(_) =>
        None
    } flatten
  }
}
