package com.secretapp.backend.persist

import com.datastax.driver.core.{ Cluster, HostDistance, PoolingOptions }
import com.datastax.driver.core.policies.{ ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy }
import com.typesafe.config._
import com.websudos.phantom.Implicits._
import java.util.concurrent.Executor
import scala.collection.JavaConversions._
import scala.concurrent. { blocking, Future }
import scala.concurrent.ExecutionContext

object DBConnector {
  val serverConfig = ConfigFactory.load().getConfig("actor-server")

  val dbConfig = serverConfig.getConfig("cassandra")

  val keySpace = dbConfig.getString("keyspace")

  val akkaDbConfig = serverConfig.getConfig("cassandra-journal")

  val akkaKeySpace = akkaDbConfig.getString("keyspace")

  val cluster = Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
    .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
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

  def createTables(session: Session)(implicit ec: ExecutionContext with Executor) = {
    val fileRecord = new File()(session, ec)

    Future.sequence(List(
      ApplePushCredentials.createTable(session),
      //AuthId.createTable(session),
      //AuthSession.createTable(session),
      //DeletedAuthSession.createTable(session),
      //AuthSmsCode.createTable(session),
      GooglePushCredentials.createTable(session),
      Group.createTable(session),
      GroupUser.createTable(session),
      //Phone.createTable(session),
      SeqUpdate.createTable(session),
      UnregisteredContact.createTable(session),
      contact.UserContactsList.createTable(session),
      contact.UserContactsListCache.createTable(session),
      UserGroup.createTable(session),
      //UserPhone.createTable(session),
      UserEmail.createTable(session),
      //UserPublicKey.createTable(session),
      //User.createTable(session),
      Dialog.createTable(session),
      DialogUnreadCounter.createTable(session),
      HistoryMessage.createTable(session),
      fileRecord.createTable(session)
    ))
  }
}
