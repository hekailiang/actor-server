package com.secretapp.backend.persist

import com.datastax.driver.core.policies.{ ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy }
import java.util.concurrent.Executor
import com.secretapp.backend.persist.contact._
import scala.concurrent. { blocking, Future }
import scala.collection.JavaConversions._
import com.datastax.driver.core.Cluster
import com.websudos.phantom.Implicits._
import com.typesafe.config._
import scala.concurrent.ExecutionContext

object DBConnector {
  val dbConfig = ConfigFactory.load().getConfig("cassandra")

  val keySpace = dbConfig.getString("keyspace")

  val cluster =  Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withPort(dbConfig.getInt("port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
    .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
    .build()

  lazy val session = blocking {
    cluster.connect(keySpace)
  }

  def createTables(session: Session)(implicit context: ExecutionContext with Executor) = {
    val fileRecord = new FileRecord()(session, context)

    Future.sequence(List(
      ApplePushCredentials.createTable(session),
      AuthIdRecord.createTable(session),
      AuthItemRecord.createTable(session),
      DeletedAuthItemRecord.createTable(session),
      AuthSmsCodeRecord.createTable(session),
      GooglePushCredentialsRecord.createTable(session),
      GroupRecord.createTable(session),
      GroupUserRecord.createTable(session),
      PhoneRecord.createTable(session),
      SeqUpdateRecord.createTable(session),
      UnregisteredContactRecord.createTable(session),
      UserContactsListRecord.createTable(session),
      UserContactsListCacheRecord.createTable(session),
      UserGroupsRecord.createTable(session),
      UserPublicKeyRecord.createTable(session),
      UserRecord.createTable(session),
      fileRecord.createTable(session)
    ))
  }

  def truncateTables(session: Session) = {
    Future.sequence(List(
      ApplePushCredentials.truncateTable(session),
      AuthIdRecord.truncateTable(session),
      AuthItemRecord.truncateTable(session),
      DeletedAuthItemRecord.truncateTable(session),
      AuthSmsCodeRecord.truncateTable(session),
      GooglePushCredentialsRecord.truncateTable(session),
      GroupRecord.truncateTable(session),
      GroupUserRecord.truncateTable(session),
      PhoneRecord.truncateTable(session),
      SeqUpdateRecord.truncateTable(session),
      UnregisteredContactRecord.truncateTable(session),
      UserContactsListRecord.truncateTable(session),
      UserContactsListCacheRecord.truncateTable(session),
      UserGroupsRecord.truncateTable(session),
      UserPublicKeyRecord.truncateTable(session),
      UserRecord.truncateTable(session)
    ))
  }

//  def dumpKeySpace() = blocking {
//    session.execute(s"DESCRIBE KEYSPACE $secret;")
//  }

}
