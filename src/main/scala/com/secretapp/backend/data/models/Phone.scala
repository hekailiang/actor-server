package com.secretapp.backend.data.models

import scala.concurrent.Future
import com.datastax.driver.core.Session
import com.secretapp.backend.persist._

case class Phone(number: Long, userId: Int) {
  def user(implicit session: Session, ex: scala.concurrent.ExecutionContext): Future[Option[User]] = {
    UserRecord.getEntity(userId)
  }
}
