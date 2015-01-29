package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import scala.concurrent.Future

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }

sealed class GooglePushCredentials extends CassandraTable[GooglePushCredentials, models.GooglePushCredentials] {

  override val tableName = "google_push_credentials"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object projectId extends LongColumn(this) {
    override lazy val name = "project_id"
  }
  object regId extends StringColumn(this) with Index[String] {
    override lazy val name = "reg_id"
  }

  override def fromRow(r: Row): models.GooglePushCredentials =
    models.GooglePushCredentials(authId(r), projectId(r), regId(r))
}

object GooglePushCredentials extends GooglePushCredentials with TableOps {

  def set(c: models.GooglePushCredentials)(implicit s: Session): Future[ResultSet] = {
    @inline def doInsert() =
      insert.value(_.authId, c.authId).value(_.projectId, c.projectId).value(_.regId, c.regId).future()

    select(_.authId).where(_.regId eqs c.regId).one() flatMap {
      case Some(authId) =>
        delete.where(_.authId eqs authId).future() flatMap { _ =>
          doInsert()
        }
      case None =>
        doInsert()
    }
  }


  def remove(authId: Long)(implicit s: Session): Future[ResultSet] =
    delete
      .where(_.authId eqs authId)
      .future

  def get(authId: Long)(implicit s: Session): Future[Option[models.GooglePushCredentials]] =
    select
      .where(_.authId eqs authId)
      .one()

  def main(args: Array[String]) {
    implicit val session = DBConnector.session
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    //DBConnector.flyway.migrate()
    println("migrated")

    val fails = moveToSQL()

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }

  def moveToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee =
      Iteratee.fold[models.GooglePushCredentials, List[Try[Unit]]](List.empty) {
        case (moves, models.GooglePushCredentials(authId, projectId, regId)) =>

        moves :+ Try {
          //val exists =
          //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
          //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

          //if (!exists) {
            sql"""insert into google_push_credentials (auth_id, project_id, reg_id)
                  VALUES (${authId}, ${projectId}, $regId)
            """.execute.apply
          //}

          ()
        }
      }

    val tries = Await.result(select.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case Failure(e) =>
        Some(e)
      case Success(_) =>
        None
    } flatten
  }
}
