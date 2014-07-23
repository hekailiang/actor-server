package com.secretapp.backend.persist

import org.specs2.matcher.ThrownExpectations
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import com.datastax.driver.core.{ Cluster, Session }
import com.typesafe.config._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.typesafe.scalalogging.slf4j._
import org.slf4j.LoggerFactory
import org.specs2.mutable._
import org.specs2.specification.{ Fragments, Step }

trait CassandraSpecification extends SpecificationLike with ThrownExpectations {
  protected val keySpace: String = s"secret_test_${System.nanoTime()}"
  private val dbConfig = ConfigFactory.load().getConfig("cassandra")
  private val cassndraSpecLog = Logger(LoggerFactory.getLogger(this.getClass))

  private val cluster = Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  implicit val session: Session = blocking {
    cluster.connect()
  }

  private def createKeySpace(spaceName: String)(implicit session: Session) = {
    blocking {
      cassndraSpecLog.info(s"CREATE KEYSPACE IF NOT EXISTS $spaceName")
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};")
      session.execute(s"use $spaceName;")
    }
  }

  private def dropKeySpace(spaceName: String)(implicit session: Session) = {
    blocking {
      cassndraSpecLog.info(s"DROP KEYSPACE IF EXISTS $spaceName")
      session.execute(s"DROP KEYSPACE IF EXISTS $spaceName;")
      session.execute("DROP KEYSPACE IF EXISTS akka;")
    }
  }

  private def createDB() {
    createKeySpace(keySpace)
    DBConnector.createTables(session).sync()
  }

  private def cleanDB() {
    dropKeySpace(keySpace)
  }

  override def map(fs: => Fragments) = Step(createDB) ^ super.map(fs) ^ Step(cleanDB)
}
