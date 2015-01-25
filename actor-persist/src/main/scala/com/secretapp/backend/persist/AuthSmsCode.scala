package com.secretapp.backend.persist

import com.datastax.driver.core.{ Session => CSession }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }
import scalikejdbc._

sealed class AuthSmsCode extends CassandraTable[AuthSmsCode, models.AuthSmsCode] {
  override val tableName = "auth_sms_codes"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }
  object smsHash extends StringColumn(this) { // TODO: with PartitionKey[Long]
    override lazy val name = "sms_hash"
  }
  object smsCode extends StringColumn(this) {
    override lazy val name = "sms_code"
  }

  override def fromRow(row: Row): models.AuthSmsCode =
    models.AuthSmsCode(phoneNumber(row), smsHash(row), smsCode(row))
}

object AuthSmsCode extends AuthSmsCode with TableOps {
  def insertEntity(entity: models.AuthSmsCode)(implicit session: CSession): Future[models.AuthSmsCode] =
    insert
      .value(_.phoneNumber, entity.phoneNumber)
      .value(_.smsHash, entity.smsHash)
      .value(_.smsCode, entity.smsCode)
      .ttl(15.minutes.toSeconds.toInt)
      .future().map(_ => entity)

  def getEntity(phoneNumber: Long)(implicit session: CSession): Future[Option[models.AuthSmsCode]] =
    select.where(_.phoneNumber eqs phoneNumber).one()

  def dropEntity(phoneNumber: Long)(implicit session: CSession) =
    delete.where(_.phoneNumber eqs phoneNumber).future()

  def list(startPhoneExclusive: Long, count: Int)(implicit session: CSession): Future[Seq[models.AuthSmsCode]] =
    select.where(_.phoneNumber gtToken startPhoneExclusive).limit(count).fetch()

  def list(count: Int)(implicit session: CSession): Future[Seq[models.AuthSmsCode]] =
    select.one flatMap {
      case Some(first) => select.where(_.phoneNumber gteToken first.phoneNumber).limit(count).fetch()
      case _           => Future.successful(Seq())
    }

  def list(startPhoneExclusive: Option[Long], count: Int)(implicit session: CSession): Future[Seq[models.AuthSmsCode]] =
    startPhoneExclusive.fold(list(count))(list(_, count))

  def main(args: Array[String]) {
    implicit val session = DBConnector.session
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    DBConnector.flyway.migrate()
    println("migrated")

    val fails = moveToSQL()

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }

  def moveToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee =
      Iteratee.fold[models.AuthSmsCode, List[Try[Unit]]](List.empty) {
        case (moves, c) =>

          moves :+ Try {
            sql"""insert into auth_sms_codes (phone_number, sms_hash, sms_code)
                VALUES (${c.phoneNumber}, ${c.smsHash}, ${c.smsCode})"""
              .execute.apply

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
