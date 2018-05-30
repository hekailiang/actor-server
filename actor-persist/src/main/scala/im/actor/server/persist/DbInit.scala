package im.actor.server.persist

import com.typesafe.config._
import java.util.concurrent.TimeUnit
import scalikejdbc._

trait DbInit {
  protected def sqlConfig: Config

  def initDb(sqlConfig: Config) = {
    Class.forName(sqlConfig.getString("driverClassName"))

    GlobalSettings.loggingSQLAndTime = GlobalSettings.loggingSQLAndTime.copy(singleLineMode = true)

    val (url, user, password) = (
      sqlConfig.getString("url"),
      sqlConfig.getString("username"),
      sqlConfig.getString("password")
    )

    val settings = ConnectionPoolSettings(
      initialSize = sqlConfig.getInt("pool.initial-size"),
      maxSize = sqlConfig.getInt("pool.max-size"),
      connectionTimeoutMillis = sqlConfig.getDuration("pool.connection-timeout", TimeUnit.MILLISECONDS),
      validationQuery = "select 1")

    ConnectionPool.singleton(url, user, password, settings)
  }
}
