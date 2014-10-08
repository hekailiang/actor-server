package com.secretapp.backend.persist

import org.specs2.matcher.ThrownExpectations
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import com.datastax.driver.core.{ Cluster, Session => CSession }
import com.typesafe.config._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.typesafe.scalalogging.slf4j._
import org.slf4j.LoggerFactory
import org.specs2.mutable._
import org.specs2.specification.{ Fragments, Step }

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

  private def createAndUseKeySpace(spaceName: String)(implicit session: CSession) = {
    blocking {
      cassandraSpecLog.info(s"Creating keyspace $spaceName")
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};")
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
    createAndUseKeySpace(keySpace)
    DBConnector.createTables(csession).sync()
  }

  private def cleanDB() {
    dropKeySpaces(keySpace)
  }

  override def map(fs: => Fragments) = Step(createDB) ^ super.map(fs) ^ Step(cleanDB)
}
