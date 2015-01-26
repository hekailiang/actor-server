package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.concurrent._, duration._
import scala.language.postfixOps

import play.api.libs.iteratee._
import scala.util.{ Try, Failure, Success }
import scalikejdbc._

sealed class AuthId extends CassandraTable[AuthId, models.AuthId] {
  override val tableName = "auth_ids"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object userId extends OptionalIntColumn(this) {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): models.AuthId = models.AuthId(authId(row), userId(row))
}

object AuthId extends AuthId with TableOps {
  def insertEntity(item: models.AuthId)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.authId, item.authId)
      .value(_.userId, item.userId)
      .future()

  def getEntity(authId: Long)(implicit session: Session): Future[Option[models.AuthId]] =
    select.where(_.authId eqs authId).one()

  def deleteEntity(authId: Long)(implicit session: Session): Future[ResultSet] =
    delete.where(_.authId eqs authId).future()

  def getEntityWithUser(authId: Long)
                       (implicit session: Session): Future[Option[(models.AuthId, Option[models.User])]] = {
    def user(a: models.AuthId): Future[Option[models.User]] =
      a.userId match {
        case Some(uid) => User.getEntity(uid, a.authId)
        case None => Future.successful(None)
      }

    getEntity(authId) flatMap {
      case Some(auth) => user(auth) map { u => Some(auth, u)}
      case None => Future.successful(None)
    }
  }

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
    val moveIteratee: Iteratee[models.AuthId, List[Try[Boolean]]] =
      Iteratee.fold[models.AuthId, List[Try[Boolean]]](List.empty) {
        (moves, authId) =>

        moves :+ Try {
          sql"insert into auth_ids (id, user_id) values (${authId.authId}, ${authId.userId})"
            .execute.apply
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
