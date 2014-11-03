package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scala.concurrent.{ExecutionContext, Future}
import com.secretapp.backend.models

private[persist] class FileSourceBlockRecord(implicit session: Session, context: ExecutionContext with Executor)
    extends CassandraTable[FileSourceBlockRecord, models.FileSourceBlock] with TableOps {
  override val tableName = "file_source_blocks"

  object fileId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "file_id"
  }
  object offset extends IntColumn(this) with PrimaryKey[Int]
  object length extends IntColumn(this)

  override def fromRow(row: Row): models.FileSourceBlock =
    models.FileSourceBlock(fileId(row), offset(row), length(row))

  def insertEntity(block: models.FileSourceBlock): Future[ResultSet] =
    insert
      .value(_.fileId, block.fileId)
      .value(_.offset, block.offset)
      .value(_.length, block.length)
      .future()

  private val countQuery = new ExecutablePreparedStatement {
    val query = s"SELECT count(*) FROM ${tableName} WHERE file_id = ?;"
  }

  def countBlocks(fileId: Int): Future[Long] = {
    countQuery.execute(fileId.asInstanceOf[java.lang.Integer]).map(_.one.getLong(0))
  }

  def getFileLength(fileId: Int): Future[Int] = {
    select(_.length).where(_.fileId eqs fileId).fetch() map { lengths =>
      lengths.foldLeft(0)(_ + _)
    }
  }
}
