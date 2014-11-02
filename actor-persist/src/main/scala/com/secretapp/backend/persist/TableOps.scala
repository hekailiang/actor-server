package com.secretapp.backend.persist

import scala.concurrent.Future
import com.websudos.phantom.Implicits._

trait TableOps {
  self: CassandraTable[_, _] =>

  def createTable(session: Session): Future[Unit] =
    create.future()(session) map (_ => ())

  def truncateTable(session: Session): Future[Unit] =
    truncate.future()(session) map (_ => ())
}
