package com.secretapp.backend.persist

import scala.concurrent.blocking
import com.datastax.driver.core.{ Cluster, Session }
import com.newzly.util.testing.cassandra.BaseTest
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.typesafe.config._

trait CassandraSpec extends BaseTest {

  lazy val keySpace: String = s"secret_test_${System.nanoTime()}"
  val dbConfig = ConfigFactory.load().getConfig("secret.persist.cassandra")

  override val cluster =  Cluster.builder()
    .addContactPoint(dbConfig.getString("contact-point.host"))
    .withPort(dbConfig.getInt("contact-point.port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  private def createKeySpace(spaceName: String) = {
    blocking {
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};")
      session.execute(s"use $spaceName;")
    }
  }

  private def dropKeySpace(spaceName: String) = {
    blocking {
      session.execute(s"DROP KEYSPACE IF EXISTS $spaceName;")
    }
  }

  /**
    * Need to override it because original method cannot find cassandra.yaml or fails for some another reason
    */
  override def beforeAll(): Unit = {
    dropKeySpace(keySpace)
    createKeySpace(keySpace)
    DBConnector.createTables(session).sync()
  }


  override def afterAll(): Unit = {
    dropKeySpace(keySpace)
  }

}
