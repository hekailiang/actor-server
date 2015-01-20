package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._

object FileData extends SQLSyntaxSupport[models.FileData] {
  override val tableName = "file_datas"
  override val columnNames = Seq(
    "id",
    "access_salt",
    "uploaded_blocks_count",
    "length"
  )

  lazy val fd = FileData.syntax("fd")

  def apply(fd: SyntaxProvider[models.FileData])(rs: WrappedResultSet): models.FileData = apply(fd.resultName)(rs)

  def apply(fd: ResultName[models.FileData])(rs: WrappedResultSet): models.FileData = models.FileData(
    id = rs.long(fd.id),
    accessSalt = rs.string(fd.accessSalt),
    uploadedBlocksCount = rs.int(fd.uploadedBlocksCount),
    length = rs.long(fd.length)
  )

  def find(id: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileData.autoSession
  ): Future[Option[models.FileData]] = Future {
    blocking {
      withSQL {
        select.from(FileData as fd)
          .where.eq(fd.id, id)
      }.map(FileData(fd)).single.apply
    }
  }

  def incrementUploadedBlocksCount(id: Long, addedLength: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileData.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(FileData).set(
          column.uploadedBlocksCount -> sqls"${column.uploadedBlocksCount} + 1",
          column.length -> sqls"${column.length} + ${addedLength}"
        )
          .where.eq(column.id, id)
      }.update.apply
    }
  }

  def create(id: Long, accessSalt: String)(
    implicit ec: ExecutionContext, session: DBSession = FileData.autoSession
  ): Future[models.FileData] = Future {
    blocking {
      withSQL {
        insert.into(FileData).namedValues(
          column.id -> id,
          column.accessSalt -> accessSalt
        )
      }.execute.apply

      models.FileData(id, accessSalt, 0, 0)
    }
  }
}
