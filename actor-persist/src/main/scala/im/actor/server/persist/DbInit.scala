package im.actor.server.persist

import com.typesafe.config._
import scalikejdbc._, async._

trait DbInit {
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
