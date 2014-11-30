package utils

import com.datastax.driver.core.SocketOptions
import com.secretapp.backend.persist
import com.datastax.driver.core.policies.{ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy}
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory
import com.websudos.phantom.Implicits._
import play.api.Logger
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, blocking}

object DbConnector {
  private val dbConfig = ConfigFactory.load().getConfig("actor-server.cassandra")

  val keySpace = dbConfig.getString("keyspace")

  val cluster =
    Cluster.builder()
      .addContactPoints(dbConfig.getStringList("contact-points").asScala :_*)
      .withPort(dbConfig.getInt("port"))
      .withoutJMXReporting()
      .withoutMetrics()
      .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
      .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
      .withSocketOptions(new SocketOptions().setReadTimeoutMillis(dbConfig.getInt("read-timeout-millis")))
      .build()

  // TODO: Get rid of lazy, it is not free.
  lazy val session = cluster.connect(keySpace)

  def createTables(session: Session)(implicit context: ExecutionContext) =
    blocking {
      Logger.trace("Creating tables")
      Future.sequence(List(
        persist.User.createTable(session),
        persist.AuthSmsCode.createTable(session),
        persist.Phone.createTable(session),
        persist.UserPublicKey.createTable(session),
        persist.AuthId.createTable(session)
      ))
    }

  def truncateTables(session: Session) =
    blocking {
      Future.sequence(List(
        persist.User.truncateTable(session),
        persist.AuthSmsCode.truncateTable(session),
        persist.Phone.truncateTable(session),
        persist.UserPublicKey.truncateTable(session),
        persist.AuthId.truncateTable(session)
      ))
    }

  object Implicits {
    implicit lazy val s = session
  }
}
