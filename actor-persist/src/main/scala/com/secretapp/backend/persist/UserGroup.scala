package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import scala.concurrent.Future

sealed class UserGroup extends CassandraTable[UserGroup, Int] {
  override val tableName = "user_group"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object groupId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "group_id"
  }

  override def fromRow(row: Row): Int = groupId(row)
}

object UserGroup extends UserGroup with TableOps {
  def addGroup(userId: Int, groupId: Int)(implicit session: Session): Future[ResultSet] =
    insert.value(_.userId, userId).value(_.groupId, groupId).future()

  def removeGroup(userId: Int, groupId: Int)(implicit session: Session): Future[ResultSet] =
    delete.where(_.userId eqs userId).and(_.groupId eqs groupId).future()

  def getGroups(userId: Int)(implicit session: Session): Future[Seq[Int]] =
    select.where(_.userId eqs userId).fetch()
}
