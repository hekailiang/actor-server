package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scala.language.postfixOps
import scala.util.{ Try, Success, Failure }
import scalikejdbc._

object FileBlock extends SQLSyntaxSupport[models.FileBlock] {
  override val tableName = "file_blocks"
  override val columnNames = Seq(
    "file_id",
    "offset_",
    "length"
  )

  lazy val fb = FileBlock.syntax("fb")

  def apply(fb: SyntaxProvider[models.FileBlock])(rs: WrappedResultSet): models.FileBlock = apply(fb.resultName)(rs)

  def apply(fd: ResultName[models.FileBlock])(rs: WrappedResultSet): models.FileBlock = models.FileBlock(
    fileId = rs.long(fb.fileId),
    offset = rs.long(fb.column("offset_")),
    length = rs.long(fb.length)
  )

  def existsSync(fileId: Long, offset: Long, length: Long)(
    implicit session: DBSession
  ): Boolean =
    sql"""
      select exists (
        select 1 from ${FileBlock.table}
        where ${column.fileId} = ${fileId} and ${column.column("offset_")} = ${offset} and ${column.length} = ${length}
      )
      """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)

  def createIfNotExists(fileId: Long, offset: Long, length: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileBlock.autoSession
  ): Future[Boolean] = Future {
    blocking {
      Try {
        withSQL {
          insert.into(FileBlock).namedValues(
            column.fileId -> fileId,
            column.column("offset_") -> offset,
            column.length -> length
          )
        }.execute.apply
      } recover {
        case e =>
          existsSync(fileId, offset, length) match {
            case true =>
              false
            case false =>
              throw e
          }
      } get
    }
  }

  def countAll(fileId: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileBlock.autoSession
  ): Future[Long] = Future {
    blocking {
      withSQL {
        select(sqls.count).from(FileBlock as fb)
          .where.eq(fb.fileId, fileId)
      }.map(rs => rs.long(1)).single().apply.getOrElse(0)
    }
  }

  def deleteAll(fileId: Long)(
    implicit ec: ExecutionContext, session: DBSession = FileBlock.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(FileBlock).where.eq(column.fileId, fileId)
      }.execute.apply
    }
  }
}
