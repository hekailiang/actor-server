package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import java.time._
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.collection.immutable
import scalaz._; import Scalaz._
import scalikejdbc._, async._, FutureImplicits._
import scodec.bits._

object UserPublicKey extends SQLSyntaxSupport[models.UserPublicKey] with ShortenedNames {
  override val tableName = "public_keys"
  override val columnNames = Seq("user_id", "hash", "data", "auth_id", "deleted_at")

  lazy val pk = UserPublicKey.syntax("pk")
  private val isNotDeleted = sqls.isNull(pk.column("deleted_at"))
  private val isDeleted = sqls.isNotNull(pk.column("deleted_at"))

  def apply(pk: SyntaxProvider[models.UserPublicKey])(rs: WrappedResultSet): models.UserPublicKey = apply(pk.resultName)(rs)

  def apply(pk: ResultName[models.UserPublicKey])(rs: WrappedResultSet): models.UserPublicKey = models.UserPublicKey(
    userId = rs.int(pk.userId),
    hash = rs.long(pk.hash),
    data = {
      val bs = rs.binaryStream(pk.data)
      val bv = BitVector.fromInputStream(bs, chunkSizeInBytes = 8192)
      bs.close()
      bv
    },
    authId = rs.long(pk.authId)
  )

  def create(userId: Int, hash: Long, data: BitVector, authId: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[models.UserPublicKey] = for {
    _ <- withSQL {
      insert.into(UserPublicKey).namedValues(
        column.userId -> userId,
        column.hash -> hash,
        column.data -> data.toByteArray,
        column.authId -> authId
      )
    }.execute.future
  } yield models.UserPublicKey(userId, hash, data, authId)

  def findByUserIdAndHash(userId: Int, hash: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.UserPublicKey]] = withSQL {
    select.from(UserPublicKey as pk)
      .where.eq(pk.userId, userId)
      .and.eq(pk.hash, hash)
      .and.append(isNotDeleted)
  } map (UserPublicKey(pk))

  def findByUserIdAndAuthId(userId: Int, authId: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[models.UserPublicKey]] = withSQL {
    select.from(UserPublicKey as pk)
      .where.eq(pk.userId, userId)
      .and.eq(pk.authId, authId)
      .and.append(isNotDeleted)
  } map (UserPublicKey(pk))

  def findAuthIdByUserIdAndHash(userId: Int, hash: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Option[Long \/ Long]] = withSQL {
    select(column.authId, column.column("deleted_at")).from(UserPublicKey as pk)
      .where.eq(column.userId, userId)
      .and.eq(column.hash, hash)
  } map { rs =>
    val authId = rs.long(pk.resultName.authId)

    rs.get[Option[DateTime]](pk.resultName.column("deleted_at")) match {
      case Some(_) => authId.right
      case None => authId.left
    }
  }

  def findAllBy(where: SQLSyntax)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession): Future[List[models.UserPublicKey]] = withSQL {
    select.from(UserPublicKey as pk)
      .where.append(isNotDeleted).and.append(sqls"${where}")
  } map (UserPublicKey(pk))

  def findAllByUserId(userId: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[models.UserPublicKey]] = findAllBy(sqls.eq(pk.userId, userId))

  def findAllDeletedByUserId(userId: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[models.UserPublicKey]] = withSQL {
    select.from(UserPublicKey as pk)
      .where.append(isDeleted)
  } map (UserPublicKey(pk))

  def findAllHashesByUserId(userId: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[Long]] = withSQL {
    select.from(UserPublicKey as pk)
      .where.append(isNotDeleted)
      .and.eq(pk.userId, userId)
  } map (rs => rs.long(pk.resultName.hash))

  def findAllByUserIdHashPairs(pairs: immutable.Seq[(Int, Long)])(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[models.UserPublicKey]] = {
    if (pairs.isEmpty)
      throw new RuntimeException("pairs should not be empty")

    val conds = pairs map {
      case (userId, hash) =>
        sqls.eq(column.userId, userId)
          .and.eq(column.hash, hash)
    }

    val condsSql = sqls.joinWithOr(conds: _*)

    findAllBy(sqls"(${condsSql})")
  }

  def findAllAuthIdsOfActiveKeys(userId: Int)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[(Long, Long)]] = withSQL {
    select(column.hash, column.authId).from(UserPublicKey as pk)
      .where.append(isNotDeleted).eq(column.userId, userId)
  } map (rs => (rs.long(pk.resultName.hash), rs.long(pk.resultName.authId)))

  def findAllAuthIdsOfDeletedKeys(userId: Int, keyHashes: immutable.Set[Long])(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[List[(Long, Long)]] = withSQL {
    select(column.hash, column.authId).from(UserPublicKey as pk)
      .where.append(isDeleted)
      .and.eq(column.userId, userId)
      .and.in(column.hash, keyHashes.toSeq)
  } map (rs => (rs.long(pk.resultName.hash), rs.long(pk.resultName.authId)))

  def destroy(userId: Int, hash: Long)(
    implicit ec: EC, session: AsyncDBSession = AsyncDB.sharedSession
  ): Future[Int] = {
    update(UserPublicKey).set(column.column("deleted_at") -> DateTime.now)
      .where.eq(column.userId, userId)
      .and.eq(column.hash, hash)
  }
}
