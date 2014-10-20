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

sealed class UserGroupChatsRecord extends CassandraTable[UserGroupChatsRecord, Int] {
  override lazy val tableName = "user_group_chats"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object chatId extends IntColumn(this) with PrimaryKey[Int] {
    override lazy val name = "chat_id"
  }

  override def fromRow(row: Row): Int = {
    chatId(row)
  }
}

object UserGroupChatsRecord extends UserGroupChatsRecord with DBConnector {
  def addChat(userId: Int, chatId: Int)(implicit session: Session): Future[ResultSet] = {
    insert
      .value(_.userId, userId).value(_.chatId, chatId).future()
  }

  def removeChat(userId: Int, chatId: Int)(implicit session: Session): Future[ResultSet] = {
    delete.where(_.userId eqs userId).and(_.chatId eqs chatId).future()
  }

  def getChats(userId: Int)(implicit session: Session): Future[Seq[Int]] = {
    select.where(_.userId eqs userId).fetch()
  }
}
