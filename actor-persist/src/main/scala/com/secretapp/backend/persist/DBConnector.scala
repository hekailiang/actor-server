package com.secretapp.backend.persist

import com.datastax.driver.core.{ Cluster, HostDistance, PoolingOptions }
import com.datastax.driver.core.policies.{ ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy }
import com.typesafe.config._
import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scala.collection.JavaConversions._
import scala.concurrent. { blocking, Future }
import scala.concurrent.ExecutionContext

import org.flywaydb.core.Flyway
import scalikejdbc._

object DBConnector {
  val serverConfig = ConfigFactory.load().getConfig("actor-server")

  val dbConfig = serverConfig.getConfig("cassandra")

  val keySpace = dbConfig.getString("keyspace")

  val akkaDbConfig = serverConfig.getConfig("cassandra-journal")
  val akkaKeySpace = akkaDbConfig.getString("keyspace")

  val akkaSnapshotDbConfig = serverConfig.getConfig("cassandra-snapshot-store")
  val akkaSnapshotKeySpace = akkaSnapshotDbConfig.getString("keyspace")

  val cluster = Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
    .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
    .withSocketOptions(new com.datastax.driver.core.SocketOptions().setReadTimeoutMillis(60000))
    .withPoolingOptions(new PoolingOptions()
      .setMinSimultaneousRequestsPerConnectionThreshold(HostDistance.REMOTE, dbConfig.getInt("pool.min-simutaneous-requests-per-connection-treshold"))
      .setMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.REMOTE, dbConfig.getInt("pool.max-simutaneous-requests-per-connection-treshold"))
      .setCoreConnectionsPerHost(HostDistance.REMOTE, dbConfig.getInt("pool.core-connections-per-host"))
      .setMaxConnectionsPerHost(HostDistance.REMOTE, dbConfig.getInt("pool.max-connections-per-host")))
    .build()

  lazy val session = blocking {
    cluster.connect(keySpace)
  }

  lazy val akkaSession = blocking {
    cluster.connect(akkaKeySpace)
  }

  lazy val akkaSnapshotSession = blocking {
    cluster.connect(akkaSnapshotKeySpace)
  }

  def initSqlDb(sqlConfig: Config) = {
    val (url, user, password) = (
      sqlConfig.getString("url"),
      sqlConfig.getString("user"),
      sqlConfig.getString("password")
    )

    ConnectionPool.singleton(url, user, password)
  }

  def initFlyway(sqlConfig: Config) = {
    val flyway = new Flyway()
    flyway.setDataSource(sqlConfig.getString("url"), sqlConfig.getString("user"), sqlConfig.getString("password"))
    flyway.setLocations("sql.migration")
    flyway
  }

  initSqlDb(serverConfig.getConfig("sql"))

  val flyway = initFlyway(serverConfig.getConfig("sql"))
  val sqlSession = DB.autoCommitSession()

  def createTables(session: Session)(implicit ec: ExecutionContext with Executor) = {
    Future.successful(())
  }
}
