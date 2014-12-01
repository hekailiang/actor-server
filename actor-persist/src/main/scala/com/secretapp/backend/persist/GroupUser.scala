package com.secretapp.backend.persist

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery
import scala.concurrent.Future
import scalaz._
import Scalaz._

case class GroupUserMeta(inviterUserId: Int, date: Long)

sealed class GroupUser extends CassandraTable[GroupUser, Int] {
  override val tableName = "group_users"

  object groupId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "group_id"
  }
  object userId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "user_id"
  }
  object inviterUserId extends IntColumn(this) {
    override lazy val name = "inviter_user_id"
  }
  object date extends LongColumn(this)

  override def fromRow(row: Row): Int = {
    userId(row)
  }

  def fromRowWithMeta(row: Row): (Int, GroupUserMeta) = {
    (
      userId(row),
      GroupUserMeta(
        inviterUserId = inviterUserId(row),
        date = date(row)
      )
    )
  }

  def selectWithMeta: SelectQuery[GroupUser, (Int, GroupUserMeta)] =
    new SelectQuery[GroupUser, (Int, GroupUserMeta)](
      this.asInstanceOf[GroupUser],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[GroupUser].fromRowWithMeta
    )
}

object GroupUser extends GroupUser with TableOps {
  def addUser(groupId: Int, userId: Int, inviterUserId: Int, date: Long)(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.groupId, groupId)
      .value(_.userId, userId)
      .value(_.inviterUserId, inviterUserId)
      .value(_.date, date)
      .future()
  }

  def removeUser(groupId: Int, userId: Int)(implicit session: Session): Future[ResultSet] = {
    delete.where(_.groupId eqs groupId).and(_.userId eqs userId).future()
  }

  def getUserIds(groupId: Int)(implicit session: Session): Future[Seq[Int]] = {
    select.where(_.groupId eqs groupId).fetch()
  }

  def getUserIdsWithMeta(groupId: Int)(implicit session: Session): Future[Seq[Tuple2[Int, GroupUserMeta]]] = {
    selectWithMeta.where(_.groupId eqs groupId).fetch()
  }
}
