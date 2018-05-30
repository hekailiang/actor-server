package im.actor.server.rest

import akka.actor._
import akka.http.model.MediaTypes
import akka.stream.FlowMaterializer
import akka.http.Http
import akka.http.server._
import akka.http.server.Directives._
import akka.http.model._
import im.actor.server.persist.file.adapter.fs.FileStorageAdapter
import scala.util.Try
import org.apache.commons.io.IOUtils
import com.typesafe.config.Config
import im.actor.server.rest.controllers._

class HttpApiService(config: Config, fileAdapter: FileStorageAdapter)(implicit system: ActorSystem, materializer: FlowMaterializer) {
  implicit val executionContext = system.dispatcher

  val interface = Try(config.getString("rest-api.interface")).getOrElse("0.0.0.0")
  val port = Try(config.getInt("rest-api.port")).getOrElse(9000)

  val routes: Route =
    pathPrefix("logs") {
      path("auth") {
        get(LogsController.authLogs)
      } ~ path("stats") {
        get(LogsController.stats())
      }
    } ~ path("users") {
      get(UsersController.index)
    } ~ path("users" / IntNumber) { userId =>
      get(UsersController.show(userId))
    } ~ path("users" / IntNumber / "avatar" / """small|large|full""".r) { (userId, size) =>
      get(UsersController.avatar(userId, size, fileAdapter))
    } ~ path("assets" / "images" / "avatar.png") {
      get(complete(HttpEntity(MediaTypes.`image/png`, avatarBytes)))
    }

  lazy val avatarBytes = IOUtils.toByteArray(getClass.getClassLoader.getResourceAsStream("avatar.png"))

  def bind() = {
    import headers._
    import HttpMethods._

    val handler: Route = routes.andThen { r =>
      r.map {
        case RouteResult.Complete(res) =>
          RouteResult.Complete(res.withHeaders(res.headers ++ Seq(
            `Access-Control-Allow-Origin`(HttpOriginRange.`*`),
            `Access-Control-Allow-Methods`(GET, POST),
            `Access-Control-Allow-Headers`("*"),
            `Access-Control-Allow-Credentials`(true)
          )))
        case m => m
      }
    }
    Http().bind(interface = interface, port = port).startHandlingWith(handler)
  }
}

object HttpApiService {
  def start(config: Config, fileAdapter: FileStorageAdapter)
           (implicit system: ActorSystem, materializer: FlowMaterializer): Unit =
    new HttpApiService(config, fileAdapter).bind()
}
