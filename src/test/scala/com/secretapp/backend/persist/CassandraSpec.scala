package com.secretapp.backend.persist

import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import com.datastax.driver.core.{ Cluster, Session }
import com.newzly.util.testing.cassandra.BaseTest
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.typesafe.config._

object CassandraHelperSpec {

  lazy val keySpace: String = s"secret_test_${System.nanoTime()}"

  private def createKeySpace(spaceName: String)(implicit session : Session) = {
    blocking {
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};")
      session.execute(s"use $spaceName;")
    }
  }

  private def dropKeySpace(spaceName: String)(implicit session : Session) = {
    blocking {
      session.execute(s"DROP KEYSPACE IF EXISTS $spaceName;")
    }
  }

  private var isCreated = false

  def recreateSpaceWithTables(keySpace: String)(implicit session : Session): Unit = {
    if (!isCreated) {
      println(s"create $keySpace")
      isCreated = true
      dropKeySpace(keySpace)
      createKeySpace(keySpace)
      DBConnector.createTables(session).sync()
    }
    session.execute(s"use $keySpace;")
  }

}

trait CassandraSpec extends BaseTest {

  lazy val keySpace: String = CassandraHelperSpec.keySpace
  val dbConfig = ConfigFactory.load().getConfig("secret.persist.cassandra")

  override val cluster = Cluster.builder()
    .addContactPoint(dbConfig.getString("contact-point.host"))
    .withPort(dbConfig.getInt("contact-point.port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  override def beforeAll(): Unit = {
    CassandraHelperSpec.recreateSpaceWithTables(keySpace)
  }

  override def afterAll(): Unit = {
    DBConnector.truncateTables(session).sync()
  }

}
