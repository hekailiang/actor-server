package com.secretapp.backend.persist

import akka.actor.ActorContext
import com.datastax.driver.core.{ Session => CSession }

trait CassandraRecords {
  val context: ActorContext
  implicit val session: CSession

  import context._

  val fileRecord = new File
}
