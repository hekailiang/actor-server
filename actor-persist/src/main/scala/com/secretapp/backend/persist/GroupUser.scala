package com.secretapp.backend.persist

import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._

case class GroupUserMeta(inviterUserId: Int, invitedAt: DateTime)

object GroupUser extends SQLSyntaxSupport[GroupUserMeta] {
  override val tableName = "group_users"
  override val columnNames = Seq(
    "group_id",
    "user_id",
    "inviter_user_id",
    "invited_at"
  )

  lazy val gu = GroupUser.syntax("gu")

  def apply(gu: SyntaxProvider[GroupUserMeta])(rs: WrappedResultSet): GroupUserMeta = apply(gu.resultName)(rs)

  def apply(gu: ResultName[GroupUserMeta])(rs: WrappedResultSet): GroupUserMeta = GroupUserMeta(
    inviterUserId = rs.int(gu.inviterUserId),
    invitedAt = rs.get[DateTime](gu.invitedAt)
  )

  def addGroupUser(groupId: Int, userId: Int, inviterUserId: Int, invitedAt: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = GroupUser.autoSession
  ): Future[Unit] = Future {
    blocking {
      withSQL {
        insert.into(GroupUser).namedValues(
          column.column("group_id") -> groupId,
          column.column("user_id") -> userId,
          column.inviterUserId -> inviterUserId,
          column.invitedAt -> invitedAt
        )
      }.execute.apply
    }
  }

  def findGroupUserIds(groupId: Int)(
    implicit ec: ExecutionContext, session: DBSession = GroupUser.autoSession
  ): Future[List[Int]] = Future {
    blocking {
      withSQL {
        select(gu.column("user_id")).from(GroupUser as gu)
          .where.eq(gu.column("group_id"), groupId)
      }.map(rs => rs.int(column.column("user_id"))).list.apply
    }
  }

  def findUserGroupIds(userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = GroupUser.autoSession
  ): Future[List[Int]] = Future {
    blocking {
      withSQL {
        select(gu.column("group_id")).from(GroupUser as gu)
          .where.eq(gu.column("user_id"), userId)
      }.map(rs => rs.int(column.column("group_id"))).list.apply
    }
  }

  def findGroupUserIdsWithMeta(groupId: Int)(
    implicit ec: ExecutionContext, session: DBSession = GroupUser.autoSession
  ): Future[List[(Int, GroupUserMeta)]] = Future {
    blocking {
      withSQL {
        select(gu.column("user_id"), gu.inviterUserId, gu.invitedAt).from(GroupUser as gu)
          .where.eq(gu.column("group_id"), groupId)
      }.map { rs =>
        (
          rs.int(column.column("user_id")),
          GroupUserMeta(
            inviterUserId = rs.int(column.inviterUserId),
            invitedAt = rs.get[DateTime](column.invitedAt)
          )
        )
      }.list.apply
    }
  }

  def removeGroupUser(groupId: Int, userId: Int)(
    implicit ec: ExecutionContext, session: DBSession = GroupUser.autoSession
  ): Future[Boolean] = Future {
    blocking {
      withSQL {
        delete.from(GroupUser)
          .where.eq(column.column("group_id"), groupId)
          .and.eq(column.column("user_id"), userId)
      }.execute.apply
    }
  }
}
