package com.secretapp.backend.persist

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery
import scalaz._
import Scalaz._

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scalikejdbc._
import scala.concurrent._, duration._
import scala.language.postfixOps
import scala.util.{ Try, Failure, Success }

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

  def fromRowWithGroupIdAndMeta(row: Row): (Int, Int, GroupUserMeta) = {
    (
      userId(row),
      groupId(row),
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

  def selectWithGroupIdAndMeta: SelectQuery[GroupUser, (Int, Int, GroupUserMeta)] =
    new SelectQuery[GroupUser, (Int, Int, GroupUserMeta)](
      this.asInstanceOf[GroupUser],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[GroupUser].fromRowWithGroupIdAndMeta
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

  def main(args: Array[String]) {
    implicit val session = DBConnector.session
    implicit val sqlSession = DBConnector.sqlSession

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

    println("migrating")
    //DBConnector.flyway.migrate()
    println("migrated")

    val fails = moveToSQL()

    Thread.sleep(10000)

    println(fails)
    println(s"Failed ${fails.length} moves")
  }

  def moveToSQL()(implicit session: Session, dbSession: DBSession): List[Throwable] = {
    val moveIteratee =
      Iteratee.fold[(Int, Int, GroupUserMeta), List[Try[Unit]]](List.empty) {
        case (moves, (userId, groupId, meta)) =>

        moves :+ Try {
          //val exists =
          //  sql"select exists ( select 1 from group_users where group_id = ${groupId} )"
          //    .map(rs => rs.boolean(1)).single.apply.getOrElse(false)

          //if (!exists) {
            sql"""insert into group_users (group_id, user_id, inviter_user_id, invited_at)
                  VALUES (${groupId}, ${userId}, ${meta.inviterUserId}, ${new DateTime(meta.date)})
            """.execute.apply
          //}

          ()
        }
      }

    val tries = Await.result(selectWithGroupIdAndMeta.fetchEnumerator() |>>> moveIteratee, 10.minutes)

    tries map {
      case Failure(e) =>
        Some(e)
      case Success(_) =>
        None
    } flatten
  }

}
