package im.actor.server.persist

import scalikejdbc._, async._

object SqlDb {
  val pool = AsyncConnectionPool.singleton("jdbc:postgresql://localhost:5432/scalikejdbc", "sa", "sa")
}
