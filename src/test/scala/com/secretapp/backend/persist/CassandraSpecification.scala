package com.secretapp.backend.persist

import com.datastax.driver.core.{ Cluster, Session => CSession }
import com.typesafe.config._
import com.typesafe.scalalogging.slf4j._
import com.websudos.util.testing.AsyncAssertionsHelper._
import org.slf4j.LoggerFactory
import org.specs2.matcher.ThrownExpectations
import org.specs2.mutable._
import org.specs2.specification.{ Fragments, Step }
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._

trait CassandraSpecification extends SpecificationLike with ThrownExpectations {
  protected val keySpace: String = s"secret_test_${System.nanoTime()}"
  private val dbConfig = ConfigFactory.load().getConfig("cassandra")
  private val cassandraSpecLog = Logger(LoggerFactory.getLogger(this.getClass))

  private val cluster = Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  implicit val csession: CSession = blocking {
    cluster.connect()
  }

  private def createKeySpace(spaceName: String)(implicit session: CSession) = {
    cassandraSpecLog.info(s"Creating keyspace $spaceName")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};")
  }

  private def createAndUseKeySpace(spaceName: String)(implicit session: CSession) = {
    blocking {
      createKeySpace(spaceName)(session)
      session.execute(s"use $spaceName;")
    }
  }

  private def dropKeySpaces(spaceName: String)(implicit session: CSession) = {
    def dropKeyspace(spaceName: String) = {
      cassandraSpecLog.info(s"Dropping keyspace $spaceName")
      session.execute(s"DROP KEYSPACE IF EXISTS $spaceName;")
    }
    def dropKeyspaceAsync(spaceName: String) = {
      cassandraSpecLog.info(s"Dropping keyspace $spaceName")
      session.executeAsync(s"DROP KEYSPACE IF EXISTS $spaceName;")
    }

    blocking {
      dropKeyspaceAsync(spaceName)
      dropKeyspace("test_akka")
      dropKeyspace("test_akka_snapshot")
    }
  }

  private def createDB() {
    createKeySpace("test_akka")
    createKeySpace("test_akka_snapshot")
    createAndUseKeySpace(keySpace)
    Await.result(DBConnector.createTables(csession), DurationInt(20).seconds)
  }

  private def cleanDB() {
    dropKeySpaces(keySpace)
  }

  override def map(fs: => Fragments) = Step(createDB) ^ super.map(fs) ^ Step(cleanDB)
}
