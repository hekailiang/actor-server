package com.secretapp.backend.persist

import com.secretapp.backend.models
import org.joda.time.DateTime
import scala.concurrent.{ ExecutionContext, Future, blocking }
import scalikejdbc._
import scodec.bits._

case class DeviceDataItem(authId: Long, userId: Int, deviceHash: String, deviceTitle: String)

object AuthSession extends SQLSyntaxSupport[models.AuthSession] {
  override val tableName = "auth_sessions"
  override val columnNames = Seq(
    "id",
    "user_id",
    "app_id",
    "app_title",
    "auth_id",
    "public_key_hash",
    "device_hash",
    "device_title",
    "auth_time",
    "auth_location",
    "latitude",
    "longitude",
    "deleted_at"
  )

  lazy val a = AuthSession.syntax("a")
  private val isNotDeleted = sqls.isNull(a.column("deleted_at"))
  private val isDeleted = sqls.isNotNull(a.column("deleted_at"))

  def apply(a: SyntaxProvider[models.AuthSession])(rs: WrappedResultSet): models.AuthSession = apply(a.resultName)(rs)

  def apply(a: ResultName[models.AuthSession])(rs: WrappedResultSet): models.AuthSession = models.AuthSession(
    id = rs.int(a.id),
    appId = rs.int(a.appId),
    appTitle = rs.string(a.appTitle),
    authId = rs.long(a.authId),
    publicKeyHash = rs.long(a.publicKeyHash),
    deviceHash = {
      val bs = rs.binaryStream(a.deviceHash)
      val bv = BitVector.fromInputStream(bs, chunkSizeInBytes = 8192)
      bs.close()
      bv
    },
    deviceTitle = rs.string(a.deviceTitle),
    authTime = rs.get[DateTime](a.authTime),
    authLocation = rs.string(a.authLocation),
    latitude = rs.doubleOpt(a.latitude),
    longitude = rs.doubleOpt(a.longitude)
  )

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[List[models.AuthSession]] = Future {
    withSQL {
      select.from(AuthSession as a)
        .where.append(isNotDeleted).and.append(sqls"${where}")
    }.map(AuthSession(a)).list.apply
  }

  def findBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[Option[models.AuthSession]] = Future {
    withSQL {
      select.from(AuthSession as a)
        .where.append(isNotDeleted)
        .and.append(sqls"${where}")
        .limit(1)
    }.map(AuthSession(a)).single.apply
  }

  def findAllByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[List[models.AuthSession]] = findAllBy(sqls.eq(a.column("user_id"), userId))

  def findAllByUserIdAndDeviceHash(userId: Int, deviceHash: BitVector)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[List[models.AuthSession]] = findAllBy(
    sqls.eq(a.column("user_id"), userId)
      .and.eq(a.deviceHash, deviceHash.toByteArray)
  )

  def findAllDeletedByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[List[models.AuthSession]] = Future {
    withSQL {
      select.from(AuthSession as a)
        .where.append(isDeleted)
        .and.eq(column.column("user_id"), userId)
    }.map(AuthSession(a)).list.apply
  }

  def findByUserIdAndPublicKeyHash(userId: Int, publicKeyHash: Long)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[Option[models.AuthSession]] = findBy(
    sqls.eq(a.column("user_id"), userId)
      .and.eq(a.publicKeyHash, publicKeyHash)
  )

  def findByUserIdAndId(userId: Int, id: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[Option[models.AuthSession]] = findBy(
    sqls.eq(a.column("user_id"), userId)
      .and.eq(a.id, id)
  )

  def create(
    userId: Int,
    id: Int,
    appId: Int,
    appTitle: String,
    authId: Long,
    publicKeyHash: Long,
    deviceHash: BitVector,
    deviceTitle: String,
    authTime: DateTime,
    authLocation: String,
    latitude: Option[Double],
    longitude: Option[Double]
  )(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[models.AuthSession] = Future {
    withSQL {
      insert.into(AuthSession).namedValues(
        column.column("user_id") -> userId,
        column.id -> id,
        column.appId -> appId,
        column.appTitle -> appTitle,
        column.authId -> authId,
        column.publicKeyHash -> publicKeyHash,
        column.deviceHash -> deviceHash.toByteArray,
        column.deviceTitle -> deviceTitle,
        column.authTime -> authTime,
        column.authLocation -> authLocation,
        column.latitude -> latitude,
        column.longitude -> longitude
      )
    }.execute.apply
    models.AuthSession(
      id = id,
      appId = appId,
      appTitle = appTitle,
      authId = authId,
      publicKeyHash = publicKeyHash,
      deviceHash = deviceHash,
      deviceTitle = deviceTitle,
      authTime = authTime,
      authLocation = authLocation,
      latitude = latitude,
      longitude = longitude
    )
  }

  def destroy(userId: Int, id: Int)(
    implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession
  ): Future[Int] = Future {
    withSQL {
      update(AuthSession).set(column.column("deleted_at") -> DateTime.now)
        .where.eq(column.column("user_id"), userId)
        .and.eq(column.id, id)
    }.update.apply
  }

  def getDevisesData(authIds: Seq[Long])
                    (implicit ec: ExecutionContext, session: DBSession = AuthSession.autoSession): Future[Seq[DeviceDataItem]] =
    Future {
      blocking {
        authIds.toList match {
          case x :: xs =>
            withSQL {
              select(a.authId, a.column("user_id"), a.deviceTitle, a.deviceHash).from(AuthSession as a)
                .where.in(a.authId, authIds)
            }.map { rs =>
              val deviceHashBV = BitVector.fromInputStream(rs.binaryStream("device_hash"), chunkSizeInBytes = 8192)
              DeviceDataItem(
                authId = rs.long("auth_id"),
                userId = rs.int("user_id"),
                deviceTitle = rs.string("device_title"),
                deviceHash = deviceHashBV.toBase64
              )
            }.list().apply()
          case Nil => Seq()
        }
      }
  }
}
