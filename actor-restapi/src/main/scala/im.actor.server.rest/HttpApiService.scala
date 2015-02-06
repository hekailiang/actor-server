package im.actor.server.rest

import akka.actor._
import akka.stream.FlowMaterializer
import akka.http.Http
import akka.http.server._
import akka.http.server.Directives._
import im.actor.server.persist.file.adapter.fs.FileStorageAdapter
import scala.util.Try
import com.typesafe.config.Config
import im.actor.server.rest.controllers._

class HttpApiService(config: Config, fileAdapter: FileStorageAdapter)(implicit system: ActorSystem, materializer: FlowMaterializer) {
  implicit val executionContext = system.dispatcher

  val interface = Try(config.getString("rest-api.interface")).getOrElse("0.0.0.0")
  val port = Try(config.getInt("rest-api.port")).getOrElse(9000)

  val routes: Route =
    pathPrefix("logs") {
      path("auth") {
        get(LogsController.stats())
      } ~ path("stats") {
        get(LogsController.authLogs)
      }
    } ~ path("users") {
      get(UsersController.index)
    } ~ path("users" / IntNumber) { userId =>
      get(UsersController.show(userId))
    } ~ path("users" / IntNumber / "avatar" / """small|large|full""".r) { (userId, size) =>
      get(UsersController.avatar(userId, size, fileAdapter))
    }

  def bind() =
    Http().bind(interface = interface, port = port).startHandlingWith(routes)
}

object HttpApiService {
  def start(config: Config, fileAdapter: FileStorageAdapter)
           (implicit system: ActorSystem, materializer: FlowMaterializer): Unit =
    new HttpApiService(config, fileAdapter).bind()
}
