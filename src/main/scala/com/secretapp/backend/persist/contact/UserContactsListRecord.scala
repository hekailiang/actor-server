package com.secretapp.backend.persist.contact

import com.datastax.driver.core.{ ResultSet, Row, Session => CSession }
import com.secretapp.backend.models.{ contact => models }
import com.secretapp.backend.persist.TableOps
import com.secretapp.backend.data.message.struct
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps

sealed class UserContactsListRecord extends CassandraTable[UserContactsListRecord, models.UserContactsList] {
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

  override def fromRow(row: Row): models.UserContactsList = {
    models.UserContactsList(
      ownerId = ownerId(row),
      contactId = contactId(row),
      phoneNumber = phoneNumber(row),
      name = name(row),
      accessSalt = accessSalt(row)
    )
  }
}

object UserContactsListRecord extends UserContactsListRecord with TableOps {
  import scalaz._
  import Scalaz._

  def insertNewContacts(userId: Int, contacts: immutable.Seq[(struct.User, String)])(implicit csession: CSession): Future[ResultSet] = {
    val batch = new BatchStatement()
    contacts foreach {
      case (contact, accessSalt) => batch.add(
        UserContactsListRecord.insert.ifNotExists()
          .value(_.ownerId, userId)
          .value(_.contactId, contact.uid)
          .value(_.phoneNumber, contact.phoneNumber)
          .value(_.name, contact.localName.getOrElse(""))
          .value(_.accessSalt, accessSalt)
      )
    }
    batch.future()
  }

  def removeContact(userId: Int, contactId: Int)(implicit csession: CSession): Future[Unit] = {
    for {
      _ <- delete.where(_.ownerId eqs userId).and(_.contactId eqs contactId).future()
      _ <- UserContactsListCacheRecord.removeContact(userId, contactId)
    } yield ()
  }

  def updateContactName(userId: Int, contactId: Int, localName: String)(implicit csession: CSession): Future[ResultSet] = {
    update.
      where(_.ownerId eqs userId).
      and(_.contactId eqs contactId).
      modify(_.name setTo localName).
      future()
  }

  def getContact(userId: Int, contactId: Int)(implicit csession: CSession): Future[Option[models.UserContactsList]] = {
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
}
