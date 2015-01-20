package utils

import org.specs2.matcher.ThrownExpectations
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.datastax.driver.core.Session
import play.api.Logger
import org.specs2.mutable._
import org.specs2.specification.{ Fragments, Step }

trait CassandraSpecification extends SpecificationLike with ThrownExpectations {

  protected implicit lazy val session: Session = DbConnector.cluster.connect()

  private def createAndUseKeySpace(keySpace: String)(implicit session: Session): Unit = {

    Logger.trace(s"Creating keyspace $keySpace")
    session.execute(
      s"""
         |CREATE KEYSPACE IF NOT EXISTS $keySpace
         |  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
      """.stripMargin
    )
    session.execute(s"use $keySpace;")
  }

  private def dropKeySpace(spaceName: String)(implicit session: Session): Unit = {
    Logger.trace(s"Dropping keyspace $spaceName")
    session.execute(s"DROP KEYSPACE IF EXISTS $spaceName;")
  }

  protected def createDb(): Unit = {
    createAndUseKeySpace(DbConnector.keySpace)
    Await.result(DbConnector.createTables(session), Duration(10, SECONDS))
  }

  protected def cleanDb(): Unit = dropKeySpace(DbConnector.keySpace)

  override def map(fs: => Fragments) =
    Step{cleanDb(); createDb()} ^ fs

}
