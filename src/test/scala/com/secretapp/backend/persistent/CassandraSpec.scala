package com.secretapp.backend.persist

import scala.concurrent.blocking
import com.newzly.util.testing.cassandra.BaseTest
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import scala.util.Random

trait CassandraSpec extends BaseTest {

  lazy val keySpace: String = s"secret_test_${new Random().nextLong.abs}"

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
    EmbeddedCassandraServerHelper.mkdirs()
    EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml")
    dropKeySpace(keySpace)
    createKeySpace(keySpace)
  }


  override def afterAll(): Unit = {
    dropKeySpace(keySpace)
  }

}
