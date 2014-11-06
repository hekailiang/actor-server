package com.secretapp.backend

import com.typesafe.config._
import scala.util.Try

object Configuration {
  implicit val serverConfig = ConfigFactory.load().getConfig("secret.server")

  val secretKey = Try(serverConfig.getString("secret-key")).getOrElse("topsecret") // TODO: WTF???
}
