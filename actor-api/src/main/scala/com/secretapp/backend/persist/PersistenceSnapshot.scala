package com.secretapp.backend.persist

import akka.actor._
import akka.persistence._
import akka.serialization._
import com.secretapp.backend.models
import com.secretapp.backend.session.SessionProtocol
import com.typesafe.config.ConfigFactory
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scodec.bits._

import akka.persistence.jdbc.util.{ Base64, EncodeDecode }
import akka.persistence.serialization.{ Snapshot => AkkaSnapshot }
import akka.serialization._
import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util

object Types {
  type SnapshotData = (String, Long, java.nio.ByteBuffer, Long)
}

import Types._

sealed class PersistenceSnapshot extends CassandraTable[PersistenceSnapshot, SnapshotData] {
  override val tableName = "snapshots"

  object processorId extends StringColumn(this) with PartitionKey[String] {
    override lazy val name = "processor_id"
  }
  object sequenceNr extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "sequence_nr"
  }
  object snapshot extends BlobColumn(this)
  object timestamp extends LongColumn(this)

  override def fromRow(row: Row): SnapshotData =
    (
      processorId(row), sequenceNr(row), snapshot(row), timestamp(row)
    )
}

object PersistenceSnapshot extends PersistenceSnapshot {
  def main(args: Array[String]) {
    implicit val session = DBConnector.akkaSnapshotSession
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    //DBConnector.flyway.migrate()
    println("migrated")

    val fails = moveToSQL()

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }

  def moveToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee =
      Iteratee.fold[SnapshotData, List[util.Try[Unit]]](List.empty) {
        case (moves, (persistenceId, sequenceNr, data, timestamp)) =>

          moves :+ util.Try {
            //val exists =
            //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
            //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

            //if (!exists) {
            sql"""
            INSERT INTO akka_snapshot (persistence_id, sequence_nr, snapshot, created) VALUES (
            ${persistenceId}, ${sequenceNr}, ${Base64.encodeString(BitVector(data).toByteArray)}, ${timestamp}
            )
            """.execute.apply
            //}

            ()
          }
      }

    val query = select
    query.tracing_=(true)

    val tries = Await.result(query.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case util.Failure(e) =>
        Some(e)
      case util.Success(_) =>
        None
    } flatten
  }
}
