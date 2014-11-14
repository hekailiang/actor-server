package persist

import com.datastax.driver.core.policies.{ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy}
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory
import com.websudos.phantom.Implicits._
import play.api.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, blocking}

object DbConnector {
  private val dbConfig = ConfigFactory.load().getConfig("cassandra")

  val keySpace = dbConfig.getString("keyspace")

  val cluster =
    Cluster.builder()
      .addContactPoints(dbConfig.getStringList("contact-points").asScala :_*)
      .withPort(dbConfig.getInt("port"))
      .withoutJMXReporting()
      .withoutMetrics()
      .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
      .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
      .build()

  // TODO: Get rid of lazy, it is not free.
  lazy val session = cluster.connect(keySpace)

  def createTables(session: Session)(implicit context: ExecutionContext) =
    blocking {
      Logger.trace("Creating tables")
      Future.sequence(List(
        persist.User.createTable(session),
        persist.AuthSmsCode.createTable(session)
      ))
    }

  def truncateTables(session: Session) =
    blocking {
      Future.sequence(List(
        persist.User.truncateTable(session),
        persist.AuthSmsCode.truncateTable(session)
      ))
    }

}

trait DbConnector {
  self: CassandraTable[_, _] =>

  implicit lazy val session: Session = DbConnector.session

  def createTable(session: Session): Future[Unit] =
    create.future()(session) map (_ => ())

  def truncateTable(session: Session): Future[Unit] =
    truncate.future()(session) map (_ => ())

}
