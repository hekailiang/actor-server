package com.secretapp.backend.persist

import akka.actor._
import akka.persistence._
import akka.serialization._
import com.secretapp.backend.models
import com.secretapp.backend.session.SessionProtocol
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scodec.bits._

sealed class PersistenceMessage extends CassandraTable[PersistenceMessage, models.PersistenceMessage] {
  override val tableName = "messages"

  object processorId extends StringColumn(this) with PartitionKey[String] {
    override lazy val name = "processor_id"
  }
  object partitionNr extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "partition_nr"
  }
  object sequenceNr extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "sequence_nr"
  }
  object marker extends StringColumn(this) with PrimaryKey[String]
  object message extends BlobColumn(this)

  override def fromRow(row: Row): models.PersistenceMessage =
    models.PersistenceMessage(
      processorId(row),
      partitionNr(row),
      sequenceNr(row),
      marker(row),
      BitVector(message(row))
    )
}

object PersistenceMessage extends PersistenceMessage {
  def processMessages(processorId: String, partitionNr: Long, optMarker: Option[String] = None)(f: models.PersistenceMessage => Any)(implicit session: Session) = {
    def process(sequenceNr: Long): Unit = {
      val baseQuery = select
        .where(_.processorId eqs processorId).and(_.partitionNr eqs partitionNr)
        .and(_.sequenceNr eqs sequenceNr)
      val query = optMarker match {
        case Some(marker) =>
          baseQuery.and(_.marker eqs marker)
        case None => baseQuery
      }

      query.one() map {
        case Some(message) =>
          f(message)
          process(sequenceNr + 1)
        case None =>
          println(s"stopped at $sequenceNr")
      }
    }

    process(1)
  }

  def upsertMessage(m: models.PersistenceMessage)(implicit session: Session): Future[ResultSet] =
    update
      .where(_.processorId eqs m.processorId)
      .and(_.partitionNr eqs m.partitionNr)
      .and(_.sequenceNr eqs m.sequenceNr)
      .and(_.marker eqs m.marker)
      .modify(_.message setTo m.message.toByteBuffer)
      .future
}