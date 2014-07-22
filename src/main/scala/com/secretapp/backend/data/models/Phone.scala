package com.secretapp.backend.data.models

import scala.concurrent.Future
import scala.collection.immutable
import com.datastax.driver.core.Session
import com.secretapp.backend.persist._
import com.secretapp.backend.data.types._

case class Phone(number: Long, userId: Int, userAccessSalt: String, userFirstName: String, userLastName: Option[String],
                 userKeyHashes: immutable.Seq[Long], userSex: Sex = NoSex) {
  def user(implicit session: Session, ex: scala.concurrent.ExecutionContext): Future[Option[User]] = {
    UserRecord.getEntity(userId)
  }
}
