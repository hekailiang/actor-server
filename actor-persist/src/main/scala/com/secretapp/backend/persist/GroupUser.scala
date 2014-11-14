package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import scala.concurrent.Future
import scalaz._
import Scalaz._

sealed class GroupUser extends CassandraTable[GroupUser, Int] {
  override val tableName = "group_users"

  object groupId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "group_id"
  }
  object userId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): Int = {
    userId(row)
  }
}

object GroupUser extends GroupUser with TableOps {
  def addUser(groupId: Int, userId: Int)(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.groupId, groupId).value(_.userId, userId).future()
  }

  def removeUser(groupId: Int, userId: Int)(implicit session: Session): Future[ResultSet] = {
    delete.where(_.groupId eqs groupId).and(_.userId eqs userId).future()
  }

  def getUserIds(groupId: Int)(implicit session: Session): Future[Seq[Int]] = {
    select.where(_.groupId eqs groupId).fetch()
  }
}
