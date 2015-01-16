package com.secretapp.backend.persist.contact

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import scala.collection.immutable
import scala.concurrent._
import scala.language.postfixOps
import scalikejdbc._

object UserContact extends SQLSyntaxSupport[models.contact.UserContact] {
  override val tableName = "user_contacts"
  override val columnNames = Seq(
    "owner_user_id",
    "contact_user_id",
    "phone_number",
    "name",
    "access_salt",
    "is_deleted"
  )

  lazy val uc = UserContact.syntax("uc")
  private val isNotDeleted = sqls.eq(uc.column("is_deleted"), false)
  private val isDeleted = sqls.eq(uc.column("is_deleted"), true)

  def apply(uc: SyntaxProvider[models.contact.UserContact])(rs: WrappedResultSet): models.contact.UserContact =
    apply(uc.resultName)(rs)

  def apply(uc: ResultName[models.contact.UserContact])(rs: WrappedResultSet): models.contact.UserContact =
    models.contact.UserContact(
      ownerUserId = rs.int(uc.ownerUserId),
      contactUserId = rs.int(uc.contactUserId),
      phoneNumber = rs.long(uc.phoneNumber),
      name = rs.string(uc.name),
      accessSalt = rs.string(uc.accessSalt)
    )

  def findBySync(where: SQLSyntax)(
    implicit session: DBSession
  ): Option[models.contact.UserContact] = withSQL {
    select.from(UserContact as uc)
      .where.append(isNotDeleted)
      .and.append(where)
      .limit(1)
  }.map(UserContact(uc)).single.apply

  def findBy(where: SQLSyntax)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Option[models.contact.UserContact]] = Future {
    blocking {
      findBySync(where)
    }
  }

  def find(ownerUserId: Int, contactUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Option[models.contact.UserContact]] = findBy(
    sqls.eq(uc.ownerUserId, ownerUserId).and.eq(uc.contactUserId, contactUserId)
  )

  def create(ownerUserId: Int, contactUserId: Int, phoneNumber: Long, name: String, accessSalt: String)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[models.contact.UserContact] = Future {
    blocking {
      withSQL {
        insert.into(UserContact).namedValues(
          column.ownerUserId -> ownerUserId,
          column.contactUserId -> contactUserId,
          column.phoneNumber -> phoneNumber,
          column.name -> name,
          column.accessSalt -> accessSalt
        )
      }.execute.apply

      models.contact.UserContact(
        ownerUserId = ownerUserId,
        contactUserId = contactUserId,
        phoneNumber = phoneNumber,
        name = name,
        accessSalt = accessSalt
      )
    }
  }

  def findAllContactIdsWithLocalNames(ownerUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[List[(Int, String)]] = Future {
    blocking {
      withSQL {
        select(uc.contactUserId, uc.name).from(UserContact as uc)
          .where.append(isNotDeleted)
          .and.eq(uc.ownerUserId, ownerUserId)
      }.map(rs =>
        (
          rs.int(column.contactUserId),
          rs.string(column.name)
        )
      ).list.apply
    }
  }

  def existsSync(ownerUserId: Int, contactUserId: Int)(
    implicit session: DBSession
  ): Boolean = {
    sql"""
      select exists (
        select 1 from ${UserContact.table}
          where is_deleted              = false
          and   ${column.ownerUserId}   = ${ownerUserId}
          and   ${column.contactUserId} = ${contactUserId}
      )
    """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)
  }

  def findAllExistingContactIdsSync(ownerUserId: Int, contactUserIds: Set[Int])(
    implicit session: DBSession
  ): List[Int] = {
    withSQL {
      select(uc.contactUserId).from(UserContact as uc)
        .where.append(isNotDeleted)
        .and.eq(uc.ownerUserId, ownerUserId)
        .and.in(uc.contactUserId, contactUserIds.toSeq)
    }.map(rs => rs.int(column.contactUserId)).list.apply
  }

  def createAll(ownerUserId: Int, contacts: immutable.Seq[(struct.User, String)])(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[List[models.contact.UserContact]] = Future {
    blocking {
      /* use batch if upsert is available (in mysql for example)
      val batchParams: Seq[Seq[Any]] = contacts map {
        case (userStruct, accessSalt) =>
          Seq(
            ownerUserId,
            contact.uid, // contactUserId
            contact.phoneNumber, // phoneNumber
            contact.localName.getOrElse(""), // name
            accessSalt
          )
      }

      sql"""
      insert into ${UserContact.table} (
        ${column.ownerUserId},
        ${column.contactUserId},
        ${column.phoneNumber},
        ${column.name},
        ${column.accessSalt}
       ) VALUES (
        ?, ?, ?, ?, ?
       )
      """.batch(batchParams: _*).apply
       */

      findAllExistingContactIdsSync(ownerUserId, contacts map (_._1.uid) toSet)
    }
  } flatMap { existingContactUserIds =>
    val futures = contacts.toList map {
      case (userStruct, accessSalt) =>
        val userContact = models.contact.UserContact(
          ownerUserId = ownerUserId,
          contactUserId = userStruct.uid,
          phoneNumber = userStruct.phoneNumber,
          name = userStruct.localName.getOrElse(""),
          accessSalt = accessSalt
        )

        if (existingContactUserIds.contains(userStruct.uid)) {
          save(userContact) map (_ => userContact)
        } else {
          create(
            ownerUserId = userContact.ownerUserId,
            contactUserId = userContact.contactUserId,
            phoneNumber = userContact.phoneNumber,
            name = userContact.name,
            accessSalt = userContact.accessSalt
          ) map (_ => userContact)
        }
      }

    Future.sequence(futures)
  }

  def save(contact: models.contact.UserContact)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(UserContact).set(
          column.phoneNumber -> contact.phoneNumber,
          column.name -> contact.name,
          column.accessSalt -> contact.accessSalt
        )
          .where.eq(column.ownerUserId, contact.ownerUserId)
          .and.eq(column.contactUserId, contact.contactUserId)
      }.update.apply
    }
  }

  def destroy(ownerUserId: Int, contactUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(UserContact as uc).set(
          column.column("is_deleted") -> true
        )
          .where.eq(uc.ownerUserId, ownerUserId)
          .and.eq(uc.contactUserId, contactUserId)
      }.update.apply
    }
  }
}
