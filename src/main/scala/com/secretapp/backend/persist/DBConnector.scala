package com.secretapp.backend.persist

import akka.dispatch.Dispatcher
import java.util.concurrent.Executor
import scala.concurrent. { blocking, Future }
import scala.collection.JavaConversions._
import com.datastax.driver.core.{ Cluster, Session }
import com.websudos.phantom.Implicits._
import com.typesafe.config._
import scala.concurrent.ExecutionContext

object DBConnector {
  val dbConfig = ConfigFactory.load().getConfig("cassandra")
  val keySpace = dbConfig.getString("keyspace")

  val cluster =  Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  lazy val session = blocking {
    cluster.connect(keySpace)
  }

  def createTables(session: Session)(implicit context: ExecutionContext with Executor): Unit = blocking {
    val fileBlockRecord = new FileBlockRecord()(session, context)

    for {
      _ <- UserRecord.createTable(session)
      _ <- AuthIdRecord.createTable(session)
      _ <- SessionIdRecord.createTable(session)
      _ <- AuthSmsCodeRecord.createTable(session)
      _ <- PhoneRecord.createTable(session)
      _ <- CommonUpdateRecord.createTable(session)
      _ <- UserPublicKeyRecord.createTable(session)
      _ <- fileBlockRecord.createTable(session)
    } yield ()
  }

  def truncateTables(session: Session) = blocking {
    for {
      _ <- UserRecord.truncateTable(session)
      _ <- AuthIdRecord.truncateTable(session)
      _ <- SessionIdRecord.truncateTable(session)
      _ <- AuthSmsCodeRecord.truncateTable(session)
      _ <- PhoneRecord.truncateTable(session)
      _ <- CommonUpdateRecord.truncateTable(session)
      _ <- UserPublicKeyRecord.truncateTable(session)
    } yield ()
  }

//  def dumpKeySpace() = blocking {
//    session.execute(s"DESCRIBE KEYSPACE $secret;")
//  }

}

trait DBConnector {
  self: CassandraTable[_, _] =>

  def createTable(session: Session): Future[Unit] = {
    create.future()(session) map (_ => ())
  }

  def truncateTable(session: Session): Future[Unit] = {
    truncate.future()(session) map (_ => ())
  }

}
