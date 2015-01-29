package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }

sealed class ApplePushCredentials extends CassandraTable[ApplePushCredentials, models.ApplePushCredentials] {

  override val tableName = "apple_push_credentials"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }

  object apnsKey extends IntColumn(this) {
    override lazy val name = "apns_key"
  }

  object token extends StringColumn(this) with Index[String] {
    override lazy val name = "tok"
  }

  override def fromRow(r: Row): models.ApplePushCredentials =
    models.ApplePushCredentials(authId(r), apnsKey(r), token(r))
}

object ApplePushCredentials extends ApplePushCredentials with TableOps {

  def set(c: models.ApplePushCredentials)(implicit s: Session): Future[ResultSet] = {
    @inline def doInsert() =
      insert.value(_.authId, c.authId).value(_.apnsKey, c.apnsKey).value(_.token, c.token).future()

    select(_.authId).where(_.token eqs c.token).one() flatMap {
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

  def get(authId: Long)(implicit s: Session): Future[Option[models.ApplePushCredentials]] =
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
      Iteratee.fold[models.ApplePushCredentials, List[Try[Unit]]](List.empty) {
        case (moves, models.ApplePushCredentials(authId, apnsKey, token)) =>

        moves :+ Try {
          //val exists =
          //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
          //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

          //if (!exists) {
            sql"""insert into apple_push_credentials (auth_id, apns_key, token)
                  VALUES (${authId}, ${apnsKey}, $token)
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
