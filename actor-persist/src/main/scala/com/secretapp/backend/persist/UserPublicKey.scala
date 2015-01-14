package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.{ Session => CSession }
import java.time._
import org.joda.time.DateTime
import scala.concurrent._
import scala.collection.immutable
import scalaz._; import Scalaz._
import scalikejdbc._
import scodec.bits._

object UserPublicKey extends SQLSyntaxSupport[models.UserPublicKey] {
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
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[models.UserPublicKey] = Future {
    blocking {
      withSQL {
        insert.into(UserPublicKey).namedValues(
          column.userId -> userId,
          column.hash -> hash,
          column.data -> data.toByteArray,
          column.authId -> authId
        )
      }.execute.apply
    }

    models.UserPublicKey(userId, hash, data, authId)
  }

  def findBySync(where: SQLSyntax)(
    implicit session: DBSession = UserPublicKey.autoSession
  ): Option[models.UserPublicKey] =
    withSQL {
      select.from(UserPublicKey as pk)
        .where.append(isNotDeleted).and.append(sqls"${where}")
        .limit(1)
    }.map(UserPublicKey(pk)).single.apply()

  def findBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[models.UserPublicKey]] =
    Future {
      blocking {
        findBySync(where)
      }
    }

  def findAllBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession): Future[List[models.UserPublicKey]] =
    Future {
      withSQL {
        select.from(UserPublicKey as pk)
          .where.append(isNotDeleted).and.append(sqls"${where}")
      }.map(UserPublicKey(pk)).list.apply()
    }

  def findByUserIdAndHash(userId: Int, hash: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[models.UserPublicKey]] =
    findBy(sqls.eq(pk.userId, userId).and.eq(pk.hash, hash))

  def findByUserIdAndHashSync(userId: Int, hash: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Option[models.UserPublicKey] =
    findBySync(sqls.eq(pk.userId, userId).and.eq(pk.hash, hash))

  def findByUserIdAndAuthId(userId: Int, authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[models.UserPublicKey]] = Future {
    blocking {
      withSQL {
        select.from(UserPublicKey as pk)
          .where.eq(pk.userId, userId)
          .and.eq(pk.authId, authId)
          .and.append(isNotDeleted)
      }.map(UserPublicKey(pk)).single.apply
    }
  }

  def findByAuthIdSync(authId: Long)(
    implicit session: DBSession = UserPublicKey.autoSession
  ): Option[models.UserPublicKey] = findBySync(
    sqls.eq(pk.authId, authId)
  )

  def findByAuthId(authId: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[models.UserPublicKey]] = Future {
    blocking { findByAuthIdSync(authId) }
  }

  def findFirstActiveAuthIdByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[Long]] = Future {
    blocking {
      withSQL {
        select(column.authId).from(UserPublicKey as pk)
          .where.append(isNotDeleted)
          .and.eq(pk.userId, userId)
          .limit(1)
      }.map(rs => rs.long(column.authId)).single.apply
    }
  }

  def findAuthIdByUserIdAndHash(userId: Int, hash: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Option[Long \/ Long]] = Future {
    blocking {
      withSQL {
        select(column.authId, column.column("deleted_at")).from(UserPublicKey as pk)
          .where.eq(column.userId, userId)
          .and.eq(column.hash, hash)
      }.map { rs =>
        val authId = rs.long(pk.resultName.authId)

        rs.get[Option[DateTime]](pk.resultName.column("deleted_at")) match {
          case Some(_) => authId.right
          case None => authId.left
        }
      }.single.apply
    }
  }

  def findAllByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[List[models.UserPublicKey]] = findAllBy(sqls.eq(pk.userId, userId))

  def findAllDeletedByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[List[models.UserPublicKey]] = Future {
    blocking {
      withSQL {
        select.from(UserPublicKey as pk)
          .where.append(isDeleted)
      }.map(UserPublicKey(pk)).list.apply
    }
  }

  def findAllHashesByUserId(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[List[Long]] = Future {
    blocking {
      withSQL {
        select.from(UserPublicKey as pk)
          .where.append(isNotDeleted)
          .and.eq(pk.userId, userId)
      }.map(rs => rs.long(column.hash)).list.apply
    }
  }

  def findAllByUserIdHashPairs(pairs: immutable.Seq[(Int, Long)])(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
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
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[List[(Long, Long)]] = Future {
    blocking {
      withSQL {
        select(column.hash, column.authId).from(UserPublicKey as pk)
          .where.append(isNotDeleted).and.eq(pk.userId, userId)
      }.map(rs => (rs.long(column.hash), rs.long(column.authId)))
        .list.apply
    }
  }

  def findAllAuthIdsOfDeletedKeys(userId: Int, keyHashes: immutable.Set[Long])(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[List[(Long, Long)]] = Future {
    blocking {
      withSQL {
        select(column.hash, column.authId).from(UserPublicKey as pk)
          .where.append(isDeleted)
          .and.eq(pk.userId, userId)
          .and.in(pk.hash, keyHashes.toSeq)
      }
        .map(rs => (rs.long(column.hash), rs.long(column.authId)))
        .list.apply
    }
  }

  def destroy(userId: Int, hash: Long)(
    implicit ec: ExecutionContext, session: DBSession = UserPublicKey.autoSession
  ): Future[Int] = Future {
    blocking {
      destroySync(userId, hash)
    }
  }

  def destroySync(userId: Int, hash: Long)(
    implicit session: DBSession = UserPublicKey.autoSession
  ): Int =
    withSQL {
      update(UserPublicKey).set(column.column("deleted_at") -> DateTime.now)
        .where.eq(column.userId, userId)
        .and.eq(column.hash, hash)
    }.update.apply
}
