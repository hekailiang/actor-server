package im.actor.server.persist.unit

import com.typesafe.config._
import org.flywaydb.core.Flyway

private[unit] trait FlywayInit {
  def initFlyway(sqlConfig: Config) = {
    val flyway = new Flyway()
    flyway.setDataSource(sqlConfig.getString("url"), sqlConfig.getString("user"), sqlConfig.getString("password"))
    flyway.setLocations("sql.migration")
    flyway
  }
}
