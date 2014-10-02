package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.types._
import scodec.bits.BitVector
import scala.concurrent.Future
import scala.collection.immutable
import scalaz._
import Scalaz._

sealed class GroupChatUserRecord extends CassandraTable[GroupChatUserRecord, Int] {
  override lazy val tableName = "group_chat_users"

  object chatId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "chat_id"
  }
  object userId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "user_id"
  }

  override def fromRow(row: Row): Int = {
    userId(row)
  }
}

object GroupChatUserRecord extends GroupChatUserRecord with DBConnector {
  def addUser(chatId: Int, userId: Int)(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.chatId, chatId).value(_.userId, userId).future()
  }

  def getUsers(chatId: Int)(implicit session: Session): Future[Seq[Int]] = {
    select.where(_.chatId eqs chatId).fetch()
  }
}
