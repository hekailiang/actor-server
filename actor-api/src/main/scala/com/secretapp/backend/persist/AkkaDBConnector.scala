package com.secretapp.backend.persist

import com.datastax.driver.core.policies.{ ConstantReconnectionPolicy, DefaultRetryPolicy, LoggingRetryPolicy }
import scala.concurrent.blocking
import scala.collection.JavaConversions._
import com.datastax.driver.core.Cluster
import com.typesafe.config._

object AkkaDBConnector {
  val dbConfig = ConfigFactory.load().getConfig("cassandra-journal")

  val keySpace = dbConfig.getString("keyspace")

  val cluster =  Cluster.builder()
    .addContactPoints(dbConfig.getStringList("contact-points") :_*)
    .withoutJMXReporting()
    .withoutMetrics()
    .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
    .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
    .build()

  lazy val session = blocking {
    cluster.connect(keySpace)
  }
}
