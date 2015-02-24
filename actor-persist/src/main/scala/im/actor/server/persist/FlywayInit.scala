package im.actor.server.persist

import com.typesafe.config._
import java.sql.{ Connection, PreparedStatement }
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.callback.FlywayCallback
import org.flywaydb.core.api.MigrationInfo

class BeforeCleanCallback extends FlywayCallback {
  def afterBaseline(connection: Connection) {}
  def afterClean(connection: Connection) {}
  def afterEachMigrate(connection: Connection) {}
  def afterInfo(connection: Connection) {}
  def afterInit(connection: Connection) {}
  def afterEachMigrate(connection: Connection, migrationInfo: MigrationInfo) {}
  def afterMigrate(connection: Connection) {}
  def afterRepair(connection: Connection) {}
  def afterValidate(connection: Connection) {}
  def beforeBaseline(connection: Connection) {}
  def beforeEachMigrate(connection: Connection, migrationInfo: MigrationInfo) {}
  def beforeInfo(connection: Connection) {}
  def beforeInit(connection: Connection) {}
  def beforeMigrate(connection: Connection) {}
  def beforeRepair(connection: Connection) {}
  def beforeValidate(connection: Connection) {}

  def beforeClean(connection: Connection) {
    if (connection.getMetaData().getDriverName().startsWith("PostgreSQL")) {
      val stmt = connection.prepareStatement("""DROP EXTENSION IF EXISTS "uuid-ossp";""")

      try {
        stmt.execute()
      } finally {
        stmt.close()
      }
    }
  }
}

trait FlywayInit {
  def initFlyway(sqlConfig: Config) = {
    Class.forName(sqlConfig.getString("driverClassName"))

    val flyway = new Flyway()
    flyway.setDataSource(sqlConfig.getString("url"), sqlConfig.getString("username"), sqlConfig.getString("password"))
    flyway.setLocations("sql.migration")
    flyway.setCallbacks(new BeforeCleanCallback())
    flyway
  }
}
