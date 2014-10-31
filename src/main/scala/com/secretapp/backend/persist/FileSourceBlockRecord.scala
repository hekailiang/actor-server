package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scala.concurrent.{ExecutionContext, Future}

case class FileSourceBlock(fileId: Int, offset: Int, length: Int)

private[persist] class FileSourceBlockRecord(implicit session: Session, context: ExecutionContext with Executor)
    extends CassandraTable[FileSourceBlockRecord, FileSourceBlock] with DBConnector {
  override val tableName = "file_source_blocks"

  object fileId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "file_id"
  }
  object offset extends IntColumn(this) with PrimaryKey[Int]
  object length extends IntColumn(this)

  override def fromRow(row: Row): FileSourceBlock =
    FileSourceBlock(fileId(row), offset(row), length(row))

  def insertEntity(block: FileSourceBlock): Future[ResultSet] =
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
