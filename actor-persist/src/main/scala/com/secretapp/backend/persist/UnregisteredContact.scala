package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }

sealed class UnregisteredContact extends CassandraTable[UnregisteredContact, models.UnregisteredContact] {
  override val tableName = "unregistered_contacts"

  object phoneNumber extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }

  object ownerUserId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "owner_user_id"
  }

  override def fromRow(row: Row): models.UnregisteredContact =
    models.UnregisteredContact(phoneNumber(row), ownerUserId(row))
}

object UnregisteredContact extends UnregisteredContact with TableOps {
  def insertEntity(uc: models.UnregisteredContact)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.phoneNumber, uc.phoneNumber)
      .value(_.ownerUserId, uc.ownerUserId)
      .future

  def byNumber(phoneNumber: Long)(implicit session: Session): Future[Set[models.UnregisteredContact]] =
    select.where(_.phoneNumber eqs phoneNumber).fetch().map(_.toSet)

  def removeEntities(phoneNumber: Long)(implicit session: Session): Future[ResultSet] =
    delete.where(_.phoneNumber eqs phoneNumber).future()

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
      Iteratee.fold[models.UnregisteredContact, List[Try[Unit]]](List.empty) {
        case (moves, uc) =>

          moves :+ Try {
            //val exists =
            //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
            //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

            //if (!exists) {
            sql"""
            INSERT INTO unregistered_contacts (phone_number, owner_user_id) VALUES (${uc.phoneNumber}, ${uc.ownerUserId})
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
