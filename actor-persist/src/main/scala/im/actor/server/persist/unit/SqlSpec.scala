package im.actor.server.persist.unit

import com.typesafe.config._
import im.actor.server.persist.{ FlywayInit, DbInit }
import org.specs2.execute.AsResult
import org.specs2.mutable.Before
import org.specs2.specification._

trait SqlSpec extends FlywayInit with DbInit {
  final val sqlConfig = ConfigFactory.load().getConfig("actor-server.sql")

  initDb(sqlConfig)

  trait sqlDb extends Scope {
    val flyway = initFlyway(sqlConfig)

    flyway.clean
    flyway.migrate
  }
}
