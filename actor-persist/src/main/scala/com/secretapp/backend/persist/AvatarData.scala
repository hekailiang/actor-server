package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalikejdbc._

object AvatarData extends SQLSyntaxSupport[models.AvatarData] {
  override val tableName = "avatar_datas"
  override val columnNames = Seq(
    "entity_id",
    "entity_type",
    "small_avatar_file_id",
    "small_avatar_file_hash",
    "small_avatar_file_size",
    "large_avatar_file_id",
    "large_avatar_file_hash",
    "large_avatar_file_size",
    "full_avatar_file_id",
    "full_avatar_file_hash",
    "full_avatar_file_size",
    "full_avatar_width",
    "full_avatar_height"
  )

  lazy val ad = AvatarData.syntax("ad")

  def apply(ad: SyntaxProvider[models.AvatarData])(rs: WrappedResultSet): models.AvatarData = apply(ad.resultName)(rs)

  def apply(ad: ResultName[models.AvatarData])(rs: WrappedResultSet): models.AvatarData = models.AvatarData(
    smallAvatarFileId = rs.intOpt(ad.smallAvatarFileId),
    smallAvatarFileHash = rs.longOpt(ad.smallAvatarFileHash),
    smallAvatarFileSize = rs.intOpt(ad.smallAvatarFileSize),
    largeAvatarFileId = rs.intOpt(ad.largeAvatarFileId),
    largeAvatarFileHash = rs.longOpt(ad.largeAvatarFileHash),
    largeAvatarFileSize = rs.intOpt(ad.largeAvatarFileSize),
    fullAvatarFileId = rs.intOpt(ad.fullAvatarFileId),
    fullAvatarFileHash = rs.longOpt(ad.fullAvatarFileHash),
    fullAvatarFileSize = rs.intOpt(ad.fullAvatarFileSize),
    fullAvatarWidth = rs.intOpt(ad.fullAvatarWidth),
    fullAvatarHeight = rs.intOpt(ad.fullAvatarHeight)
  )

  trait TypeValImpl[T] { def apply[T](): Int }
  implicit object typeValUser extends TypeValImpl[models.User] { def apply[T]() = 1 }
  implicit object typeValGroup extends TypeValImpl[models.Group] { def apply[T]() = 2 }
  def typeVal[T]()(implicit impl: TypeValImpl[T]) = impl()

  def find[T](id: Long)(
    implicit
      impl: TypeValImpl[T],
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[Option[models.AvatarData]] = find(id, typeVal[T])

  def find(id: Long, typ: Int)(
    implicit
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[Option[models.AvatarData]] = Future {
    withSQL {
      select.from(AvatarData as ad)
        .where.eq(ad.column("entity_id"), id)
        .and.eq(ad.column("entity_type"), typ)
    }.map(AvatarData(ad)).single.apply
  }

  def create[T](id: Long, data: models.AvatarData)(
    implicit
      impl: TypeValImpl[T],
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[models.AvatarData] = create(id, typeVal[T], data)

  def create(id: Long, typ: Int, data: models.AvatarData)(
    implicit
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[models.AvatarData] = Future {
    withSQL {
      insert.into(AvatarData).namedValues(
        column.column("entity_id") -> id,
        column.column("entity_type") -> typ,
        column.smallAvatarFileId -> data.smallAvatarFileId,
        column.smallAvatarFileHash -> data.smallAvatarFileHash,
        column.smallAvatarFileSize -> data.smallAvatarFileSize,
        column.largeAvatarFileId -> data.largeAvatarFileId,
        column.largeAvatarFileHash -> data.largeAvatarFileHash,
        column.largeAvatarFileSize -> data.largeAvatarFileSize,
        column.fullAvatarFileId -> data.fullAvatarFileId,
        column.fullAvatarFileHash -> data.fullAvatarFileHash,
        column.fullAvatarFileSize -> data.fullAvatarFileSize,
        column.fullAvatarWidth -> data.fullAvatarWidth,
        column.fullAvatarHeight -> data.fullAvatarHeight
      )
    }.execute.apply
    data
  }

  def save[T](id: Long, data: models.AvatarData)(
    implicit
      impl: TypeValImpl[T],
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[Int] = save(id, typeVal[T], data)

  def save(id: Long, typ: Int, data: models.AvatarData)(
    implicit
      ec: ExecutionContext, session: DBSession = AvatarData.autoSession
  ): Future[Int] = Future {
    withSQL {
      update(AvatarData).set(
        ad.smallAvatarFileId -> data.smallAvatarFileId,
        ad.smallAvatarFileHash -> data.smallAvatarFileHash,
        ad.smallAvatarFileSize -> data.smallAvatarFileSize,
        ad.largeAvatarFileId -> data.largeAvatarFileId,
        ad.largeAvatarFileHash -> data.largeAvatarFileHash,
        ad.largeAvatarFileSize -> data.largeAvatarFileSize,
        ad.fullAvatarFileId -> data.fullAvatarFileId,
        ad.fullAvatarFileHash -> data.fullAvatarFileHash,
        ad.fullAvatarFileSize -> data.fullAvatarFileSize,
        ad.fullAvatarWidth -> data.fullAvatarWidth,
        ad.fullAvatarHeight -> data.fullAvatarHeight
      )
        .where.eq(ad.column("entity_id"), id)
        .and.eq(ad.column("entity_type"), typ)
    }.update.apply
  }
}
