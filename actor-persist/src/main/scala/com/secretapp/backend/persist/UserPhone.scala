package com.secretapp.backend.persist

import com.websudos.phantom.Implicits._
import com.secretapp.backend.models
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Failure, Success }

sealed class UserPhone extends CassandraTable[UserPhone, models.UserPhone] {
  override val tableName = "user_phones"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override val name = "user_id"
  }

  object phoneId extends IntColumn(this) with PrimaryKey[Int] {
    override val name = "phone_id"
  }

  object accessSalt extends StringColumn(this) {
    override val name = "access_salt"
  }

  object number extends LongColumn(this)

  object title extends StringColumn(this)

  override def fromRow(row: Row): models.UserPhone =
    models.UserPhone(
      id = phoneId(row),
      userId = userId(row),
      accessSalt = accessSalt(row),
      number = number(row),
      title = title(row)
    )
}

object UserPhone extends UserPhone with TableOps {
  def insertEntity(entity: models.UserPhone)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.userId, entity.userId)
      .value(_.phoneId, entity.id)
      .value(_.accessSalt, entity.accessSalt)
      .value(_.number, entity.number)
      .value(_.title, entity.title)
      .future()

  def getEntity(userId: Int, phoneId: Int)(implicit session: Session): Future[Option[models.UserPhone]] =
    select.where(_.userId eqs userId).and(_.phoneId eqs phoneId).one()

  def fetchUserPhones(userId: Int)(implicit session: Session): Future[Seq[models.UserPhone]] =
    select.where(_.userId eqs userId).fetch()

  def fetchUserPhoneIds(userId: Int)(implicit session: Session): Future[Seq[Int]] =
    select(_.phoneId).where(_.userId eqs userId).fetch()

  def editTitle(userId: Int, phoneId: Int, title: String)(implicit session: Session): Future[ResultSet] =
    update
      .where(_.userId eqs userId).and(_.phoneId eqs phoneId)
      .modify(_.title setTo title).future()

  def migrate_createUserPhones()(implicit session: Session) = {
    val rand = new util.Random()

    for {
      users <- User.list(java.lang.Integer.MAX_VALUE)
    } yield {
      users foreach { user =>
        Phone.getEntity(user.phoneNumber)(session) onComplete {
          case Success(Some(phone)) =>
            val phoneId = rand.nextInt(java.lang.Integer.MAX_VALUE) + 1
            val userPhone = phone.toUserPhone(
              id = phoneId,
              userId = user.uid,
              accessSalt = rand.nextString(30)
            )
            UserPhone.insertEntity(userPhone) onComplete {
              case Success(_) =>
                User.addPhoneId(user.uid, phoneId) onFailure {
                  case e =>
                    println(s"[E] Failed to add phoneId $phoneId to user ${user.uid}")
                }

                User.setState(user.uid, models.UserState.Registered) onFailure {
                  case e =>
                    println(s"[E] Failed to set user state to Registered ${user.uid}")
                }
              case Failure(e) =>
                println(s"[E] Failed to insert $userPhone")
                throw e
            }
          case Success(None) =>
            println(s"[E] Cannot find phone for user $user")
          case Failure(e) =>
            println(s"[E] Failed to find phone for user $user")
            throw e
        }
      }
    }
  }
}
