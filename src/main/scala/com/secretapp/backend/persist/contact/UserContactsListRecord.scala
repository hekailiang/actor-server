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

  override def fromRow(row: Row): models.UserContactsList = {
    models.UserContactsList(
      ownerId = ownerId(row),
      contactId = contactId(row),
      phoneNumber = phoneNumber(row),
      name = name(row)
    )
  }
}

object UserContactsListRecord extends UserContactsListRecord with TableOps {
  import scalaz._
  import Scalaz._

  def insertEntities(userId: Int, contacts: immutable.Seq[struct.User])(implicit csession: CSession): Future[ResultSet] = {
    val batch = new BatchStatement()
    contacts foreach { contact =>
      batch.add(
        UserContactsListRecord.insert.ifNotExists()
          .value(_.ownerId, userId)
          .value(_.contactId, contact.uid)
          .value(_.phoneNumber, contact.phoneNumber)
          .value(_.name, contact.localName.getOrElse(""))
      )
    }
    batch.future()
  }

  def getEntitiesWithLocalName(userId: Int)(implicit csession: CSession): Future[Seq[(Int, String)]] = {
    select(_.contactId, _.name).where(_.ownerId eqs userId).fetch()
  }
}
