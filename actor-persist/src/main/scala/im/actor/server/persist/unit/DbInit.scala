package im.actor.server.persist.unit

import com.typesafe.config._
import scalikejdbc._, async._

private[unit] trait DbInit {
  protected def sqlConfig: Config

  def initDb(sqlConfig: Config) = {
    val (url, user, password) = (
      sqlConfig.getString("url"),
      sqlConfig.getString("user"),
      sqlConfig.getString("password")
    )

    ConnectionPool.singleton(url, user, password)
    AsyncConnectionPool.singleton(url, user, password)
  }
}
