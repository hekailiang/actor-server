package com.secretapp.backend.persist

import akka.dispatch.Dispatcher
import com.datastax.driver.core.{ResultSet, Row, Session}
import com.secretapp.backend.data.Implicits._
import com.websudos.phantom.Implicits._
import com.websudos.phantom.keys.ClusteringOrder
import java.util.concurrent.Executor
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

case class FileSourceBlock(fileId: Int, offset: Int)

private[persist] class FileSourceBlockRecord(implicit session: Session, context: ExecutionContext with Executor)
    extends CassandraTable[FileSourceBlockRecord, FileSourceBlock] with DBConnector {
  override lazy val tableName = "file_source_blocks"

  object fileId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "file_id"
  }

  object offset extends IntColumn(this) with PrimaryKey[Int]

  override def fromRow(row: Row): FileSourceBlock = {
    FileSourceBlock(fileId(row), offset(row))
  }

  def insertEntity(block: FileSourceBlock): Future[ResultSet] = {
    insert.value(_.fileId, block.fileId).value(_.offset, block.offset).future()
  }

  private val countQuery = new ExecutablePreparedStatement {
    val query = s"SELECT count(*) FROM ${tableName} WHERE file_id = ?;"
  }

  def countBlocks(fileId: Int): Future[Long] = {
    countQuery.execute(fileId.asInstanceOf[java.lang.Integer]).map(_.one.getLong(0))
  }
}
