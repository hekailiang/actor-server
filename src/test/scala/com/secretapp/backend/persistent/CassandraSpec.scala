package com.secretapp.backend.persist

import scala.concurrent.blocking
import com.newzly.util.testing.cassandra.BaseTest
import org.cassandraunit.utils.EmbeddedCassandraServerHelper

trait CassandraSpec extends BaseTest {
  private def createKeySpace(spaceName: String) = {
    blocking {
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $spaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};")
      session.execute(s"use $spaceName;")
    }
  }

  /**
    * Need to override it because original method cannot find cassandra.yaml or fails for some another reason
    */
  override def beforeAll(): Unit = {
    EmbeddedCassandraServerHelper.mkdirs()
    EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml")
    createKeySpace(keySpace)
  }
}
