package com.secretapp.backend.persist.contact

import com.secretapp.backend.models
import java.security.MessageDigest
import scala.collection.immutable
import scala.concurrent._
import scala.language.postfixOps
import scala.util.Try
import scalikejdbc._
import scodec.bits._

trait UserContactHelpers {
  private lazy val mdSha256 = MessageDigest.getInstance("SHA-256")

  def getSHA1Hash(ids: immutable.Set[Int]): String = {
    val uids = ids.to[immutable.SortedSet].mkString(",")
    BitVector(mdSha256.digest(uids.getBytes)).toHex
  }

  lazy val emptySHA1Hash = getSHA1Hash(Set())
}

object UserContact extends SQLSyntaxSupport[models.contact.UserContact] with UserContactHelpers {
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
      name = rs.stringOpt(uc.name),
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

  def findLocalName(ownerUserId: Int, contactUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Option[String]] = Future {
    blocking {
      withSQL {
        select.from(UserContact as uc)
          .where.append(isNotDeleted)
          .and.eq(uc.ownerUserId, ownerUserId)
          .and.eq(uc.contactUserId, contactUserId)
      }.map(rs => rs.stringOpt(uc.resultName.name)).single.apply.flatten
    }
  }

  def createSync(ownerUserId: Int, contactUserId: Int, phoneNumber: Long, name: Option[String], accessSalt: String)(
    implicit session: DBSession
  ): models.contact.UserContact = {
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

  def createOrRestore(ownerUserId: Int, contactUserId: Int, phoneNumber: Long, name: Option[String], accessSalt: String)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[models.contact.UserContact] = Future {
    blocking {
      Try(createSync(ownerUserId, contactUserId, phoneNumber, name, accessSalt)) recover {
        case e =>
          if (existsWithDeletedSync(ownerUserId, contactUserId)) {
            val contact = models.contact.UserContact(
              ownerUserId = ownerUserId,
              contactUserId = contactUserId,
              phoneNumber = phoneNumber,
              name = name,
              accessSalt = accessSalt
            )
            saveSync(contact, isDeleted = false)
            contact
          } else {
            throw e
          }
      } get
    }
  }

  def findAllContactIds(ownerUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[immutable.Set[Int]] = Future {
    blocking {
      withSQL {
        select(uc.contactUserId).from(UserContact as uc)
          .where.append(isNotDeleted)
          .and.eq(uc.ownerUserId, ownerUserId)
      }.map(rs => rs.int(column.contactUserId)).list.apply.toSet
    }
  }

  def findAllContactIdsAndDeleted(ownerUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[immutable.Set[Int]] = Future {
    blocking {
      withSQL {
        select(uc.contactUserId).from(UserContact as uc)
          .where.eq(uc.ownerUserId, ownerUserId)
      }.map(rs => rs.int(column.contactUserId)).list.apply.toSet
    }
  }

  def findAllContactIdsWithLocalNames(ownerUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[List[(Int, Option[String])]] = Future {
    blocking {
      withSQL {
        select(uc.contactUserId, uc.name).from(UserContact as uc)
          .where.append(isNotDeleted)
          .and.eq(uc.ownerUserId, ownerUserId)
      }.map(rs =>
        (
          rs.int(column.contactUserId),
          rs.stringOpt(column.name)
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

  def existsWithDeletedSync(ownerUserId: Int, contactUserId: Int)(
    implicit session: DBSession
  ): Boolean = {
    sql"""
      select exists (
        select 1 from ${UserContact.table}
          where ${column.ownerUserId}   = ${ownerUserId}
          and   ${column.contactUserId} = ${contactUserId}
      )
    """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)
  }

  def findAllExistingContactIdsSync(ownerUserId: Int, contactUserIds: Set[Int])(
    implicit session: DBSession = UserContact.autoSession
  ): List[Int] = {
    withSQL {
      select(uc.contactUserId).from(UserContact as uc)
        .where.append(isNotDeleted)
        .and.eq(uc.ownerUserId, ownerUserId)
        .and.in(uc.contactUserId, contactUserIds.toSeq)
    }.map(rs => rs.int(column.contactUserId)).list.apply
  }

  def saveSync(contact: models.contact.UserContact, isDeleted: Boolean)(
    implicit session: DBSession
  ): Int = {
    withSQL {
      update(UserContact).set(
        column.phoneNumber -> contact.phoneNumber,
        column.name -> contact.name,
        column.accessSalt -> contact.accessSalt,
        column.column("is_deleted") -> isDeleted
      )
        .where.eq(column.ownerUserId, contact.ownerUserId)
        .and.eq(column.contactUserId, contact.contactUserId)
    }.update.apply
  }

  def save(contact: models.contact.UserContact, isDeleted: Boolean = false)(
    implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession
  ): Future[Int] = Future {
    blocking {
      saveSync(contact, isDeleted)
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


  def getCountactsCount(userIds: Seq[Int])
                       (implicit ec: ExecutionContext, session: DBSession = UserContact.autoSession): Future[Seq[(Int, Int)]] =
    Future {
      blocking {
        sql"""
           select owner_user_id as user_id, count(*) as contacts_count
           from $table
           where owner_user_id in ($userIds)
           group by owner_user_id
           """.map { rs => (rs.int("user_id"), rs.int("contacts_count")) }.list().apply()
      }
    }
}
