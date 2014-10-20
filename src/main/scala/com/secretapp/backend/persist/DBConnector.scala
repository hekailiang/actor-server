package com.secretapp.backend.persist

import akka.dispatch.Dispatcher
import com.datastax.driver.core.policies.{ ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy }
import java.util.concurrent.Executor
import scala.concurrent. { blocking, Future }
import scala.collection.JavaConversions._
import com.datastax.driver.core.{ Cluster, Session }
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

  def createTables(session: Session)(implicit context: ExecutionContext with Executor) = blocking {
    val fileRecord = new FileRecord()(session, context)

    Future.sequence(List(
      ApplePushCredentialsRecord.createTable(session),
      AuthIdRecord.createTable(session),
      AuthSmsCodeRecord.createTable(session),
      GooglePushCredentialsRecord.createTable(session),
      GroupChatRecord.createTable(session),
      GroupChatUserRecord.createTable(session),
      PhoneRecord.createTable(session),
      SeqUpdateRecord.createTable(session),
      UnregisteredContactRecord.createTable(session),
      UserGroupChatsRecord.createTable(session),
      UserPublicKeyRecord.createTable(session),
      UserRecord.createTable(session),
      fileRecord.createTable(session)
    ))
  }

  def truncateTables(session: Session) = blocking {
    Future.sequence(List(
      ApplePushCredentialsRecord.truncateTable(session),
      AuthIdRecord.truncateTable(session),
      AuthSmsCodeRecord.truncateTable(session),
      GooglePushCredentialsRecord.truncateTable(session),
      GroupChatRecord.truncateTable(session),
      GroupChatUserRecord.truncateTable(session),
      PhoneRecord.truncateTable(session),
      SeqUpdateRecord.truncateTable(session),
      UnregisteredContactRecord.truncateTable(session),
      UserGroupChatsRecord.truncateTable(session),
      UserPublicKeyRecord.truncateTable(session),
      UserRecord.truncateTable(session)
    ))
  }

//  def dumpKeySpace() = blocking {
//    session.execute(s"DESCRIBE KEYSPACE $secret;")
//  }

}

trait DBConnector {
  self: CassandraTable[_, _] =>

  def createTable(session: Session): Future[Unit] = {
    create.future()(session) map (_ => ())
  }

  def truncateTable(session: Session): Future[Unit] = {
    truncate.future()(session) map (_ => ())
  }

}
