package com.secretapp.backend.persist

import scala.concurrent. { blocking, Future }
import com.datastax.driver.core.{ Cluster, Session }
import com.newzly.phantom.Implicits._
import com.typesafe.config._

object DBConnector {
  val keySpace = "secret"
  val dbConfig = ConfigFactory.load().getConfig("secret.persist.cassandra")

  lazy val cluster =  Cluster.builder()
    .addContactPoint(dbConfig.getString("contact-point.host"))
    .withPort(dbConfig.getInt("contact-point.port"))
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  lazy val session = blocking {
    cluster.connect(keySpace)
  }
}

trait DBConnector {
  self: CassandraTable[_, _] =>

  def createTable(session: Session): Future[Unit] = {
    create.future()(session) map (_ => ())
  }
}