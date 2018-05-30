package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._

object FileData extends SQLSyntaxSupport[models.FileData] {
  override val tableName = "file_datas"
  override val columnNames = Seq(
    "id",
    "access_salt",
    "length",
    "adapter_data"
  )

  lazy val fd = FileData.syntax("fd")

  def apply(fd: SyntaxProvider[models.FileData])(rs: WrappedResultSet): models.FileData = apply(fd.resultName)(rs)

  def apply(fd: ResultName[models.FileData])(rs: WrappedResultSet): models.FileData = models.FileData(
    id = rs.long(fd.id),
    accessSalt = rs.string(fd.accessSalt),
    length = rs.long(fd.length),
    adapterData = rs.bytes(fd.adapterData)
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

  def incrementLength(id: Long, addedLength: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileData.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(FileData).set(
          column.length -> sqls"${column.length} + ${addedLength}"
        )
          .where.eq(column.id, id)
      }.update.apply
    }
  }

  def create(id: Long, accessSalt: String, adapterData: Array[Byte])(
    implicit ec: ExecutionContext, session: DBSession = FileData.autoSession
  ): Future[models.FileData] = Future {
    blocking {
      withSQL {
        insert.into(FileData).namedValues(
          column.id -> id,
          column.accessSalt -> accessSalt,
          column.adapterData -> adapterData
        )
      }.execute.apply

      models.FileData(id, accessSalt, 0, adapterData)
    }
  }
}
