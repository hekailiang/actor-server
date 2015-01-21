package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent._
import scalikejdbc._

object UnregisteredContact extends SQLSyntaxSupport[models.UnregisteredContact] {
  override val tableName = "unregistered_contacts"
  override val columnNames = Seq(
    "phone_number",
    "owner_user_id"
  )

  lazy val uc = UnregisteredContact.syntax("uc")

  def apply(uc: SyntaxProvider[models.UnregisteredContact])(rs: WrappedResultSet): models.UnregisteredContact =
    apply(uc.resultName)(rs)

  def apply(uc: ResultName[models.UnregisteredContact])(rs: WrappedResultSet): models.UnregisteredContact =
    models.UnregisteredContact(
      phoneNumber = rs.long(uc.phoneNumber),
      ownerUserId = rs.int(uc.ownerUserId)
    )

  def findAllByPhoneNumber(phoneNumber: Long)(
    implicit ec: ExecutionContext, session: DBSession = UnregisteredContact.autoSession
  ): Future[List[models.UnregisteredContact]] = Future {
    blocking {
      withSQL {
        select.from(UnregisteredContact as uc)
          .where.eq(uc.phoneNumber, phoneNumber)
      }.map(UnregisteredContact(uc)).list.apply
    }
  }

  def existsSync(phoneNumber: Long, ownerUserId: Int)(
    implicit session: DBSession
  ): Boolean =
    sql"""
      select exists (
        select 1 from ${UnregisteredContact.table}
        where ${column.phoneNumber} = ${phoneNumber}
        and   ${column.ownerUserId} = ${ownerUserId}
      )
      """.map(rs => rs.boolean(1)).single.apply.getOrElse(false)

  def createIfNotExists(phoneNumber: Long, ownerUserId: Int)(
    implicit ec: ExecutionContext, session: DBSession = UnregisteredContact.autoSession
  ): Future[models.UnregisteredContact] = Future {
    blocking {
      existsSync(phoneNumber, ownerUserId) match {
        case false =>
          withSQL {
            insert.into(UnregisteredContact).namedValues(
              column.phoneNumber -> phoneNumber,
              column.ownerUserId -> ownerUserId
            )
          }.execute.apply
        case true =>
      }

      models.UnregisteredContact(
        phoneNumber = phoneNumber,
        ownerUserId = ownerUserId
      )
    }
  }

  def destroyAllByPhoneNumber(phoneNumber: Long)(
    implicit ec: ExecutionContext, session: DBSession = UnregisteredContact.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(UnregisteredContact as uc)
          .where.eq(uc.phoneNumber, phoneNumber)
      }
    }.execute.apply
  }
}
