package com.secretapp.backend.persist

import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import com.datastax.driver.core.{ Cluster, Session }
import org.scalatest._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.typesafe.config._

trait CassandraSpec { self: BeforeAndAfterAll with BeforeAndAfterEach =>

  lazy val keySpace: String = s"secret_test_${System.nanoTime()}"
  val dbConfig = ConfigFactory.load().getConfig("secret.persist.cassandra")

  val cluster = Cluster.builder()
    .addContactPoint(dbConfig.getString("contact-point.host"))
    .withPort(dbConfig.getInt("contact-point.port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  implicit lazy val session: Session = blocking {
    cluster.connect()
  }

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

  override def beforeAll(): Unit = {
    createKeySpace(keySpace)
    DBConnector.createTables(session).sync()
  }

  override def afterAll(): Unit = {
    dropKeySpace(keySpace)
  }

  override def afterEach(): Unit = {
    DBConnector.truncateTables(session).sync()
  }

}

trait CassandraFlatSpec extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with Assertions with CassandraSpec
trait CassandraWordSpec extends WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with Assertions with CassandraSpec
