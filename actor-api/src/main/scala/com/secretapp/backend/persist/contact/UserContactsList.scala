package com.secretapp.backend.persist.contact

import com.datastax.driver.core.{ ResultSet, Row, Session => CSession }
import com.secretapp.backend.models
import com.secretapp.backend.persist.{ DBConnector, TableOps }
import com.secretapp.backend.data.message.struct
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.language.postfixOps

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util


sealed class UserContactsList extends CassandraTable[UserContactsList, models.contact.UserContactsList] {
  override lazy val tableName = "user_contacts_lists"

  object ownerId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "owner_id"
  }
  object contactId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "contact_id"
  }
  object phoneNumber extends LongColumn(this) {
    override lazy val name = "phone_number"
  }
  object name extends StringColumn(this)
  object accessSalt extends StringColumn(this) {
    override lazy val name = "access_salt"
  }

  override def fromRow(row: Row): models.contact.UserContactsList = {
    models.contact.UserContactsList(
      ownerId = ownerId(row),
      contactId = contactId(row),
      phoneNumber = phoneNumber(row),
      name = name(row),
      accessSalt = accessSalt(row)
    )
  }
}

object UserContactsList extends UserContactsList with TableOps {
  import scalaz._
  import Scalaz._

  def insertNewContacts(userId: Int, contacts: immutable.Seq[(struct.User, String)])(implicit csession: CSession): Future[ResultSet] = {
    val batch = new BatchStatement()
    contacts foreach {
      case (contact, accessSalt) => batch.add(
        UserContactsList.insert.ifNotExists()
          .value(_.ownerId, userId)
          .value(_.contactId, contact.uid)
          .value(_.phoneNumber, contact.phoneNumber)
          .value(_.name, contact.localName.getOrElse(""))
          .value(_.accessSalt, accessSalt)
      )
    }
    batch.future()
  }

  def insertContact(userId: Int, contactId: Int, phoneNumber: Long, localName: String, accessSalt: String)
                   (implicit csession: CSession): Future[ResultSet] = {
    insert
      .value(_.ownerId, userId)
      .value(_.contactId, contactId)
      .value(_.phoneNumber, phoneNumber)
      .value(_.name, localName)
      .value(_.accessSalt, accessSalt)
      .future()
  }

  def removeContact(userId: Int, contactId: Int)(implicit csession: CSession): Future[Unit] = {
    for {
      _ <- delete.where(_.ownerId eqs userId).and(_.contactId eqs contactId).future()
      _ <- UserContactsListCache.removeContact(userId, contactId)
    } yield ()
  }

  def updateContactName(userId: Int, contactId: Int, localName: String)(implicit csession: CSession): Future[ResultSet] = {
    update.
      where(_.ownerId eqs userId).
      and(_.contactId eqs contactId).
      modify(_.name setTo localName).
      future()
  }

  def getContact(userId: Int, contactId: Int)(implicit csession: CSession): Future[Option[models.contact.UserContactsList]] = {
    select.
      where(_.ownerId eqs userId).
      and(_.contactId eqs contactId).
      one()
  }

  def getEntitiesWithLocalName(userId: Int)(implicit csession: CSession): Future[Seq[(Int, String)]] = {
    select(_.contactId, _.name).
      where(_.ownerId eqs userId).
      fetch()
  }

  def moveCacheToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee =
      Iteratee.fold[models.contact.UserContactsListCache, List[util.Try[Unit]]](List.empty) {
        case (moves, uc) =>

          moves :+ util.Try {
            //val exists =
            //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
            //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

            //if (!exists) {
            val ids: List[Int] = uc.deletedContactsId.toList

            ids.foreach { id =>
              val updatedRowsCount = sql"""
              INSERT INTO user_contacts (owner_user_id, contact_user_id, phone_number, name, access_salt, is_deleted) VALUES (
              ${uc.ownerId}, ${id}, 0, '', '', true
              )
              """.update.apply
            }

            ()
          }
      }

    val tries = Await.result(UserContactsListCache.enumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case util.Failure(e) =>
        Some(e)
      case util.Success(_) =>
        None
    } flatten
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
    val moveIteratee =
      Iteratee.fold[models.contact.UserContactsList, List[util.Try[Unit]]](List.empty) {
        case (moves, uc) =>

          moves :+ util.Try {
            //val exists =
            //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
            //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

            //if (!exists) {
            sql"""
            INSERT INTO user_contacts (owner_user_id, contact_user_id, phone_number, name, access_salt, is_deleted) VALUES (
            ${uc.ownerId}, ${uc.contactId}, ${uc.phoneNumber}, ${uc.name}, ${uc.accessSalt}, false
            )
            """.execute.apply
            //}

            ()
          }
      }

    val tries = Await.result(select.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    val cacheTries = moveCacheToSQL()

    (tries ++ cacheTries) map {
      case util.Failure(e) =>
        Some(e)
      case util.Success(_) =>
        None
    } flatten
  }
}
