import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {
  override def doFilter(next: EssentialAction): EssentialAction = EssentialAction { req =>
    next.apply(req).map(_.withHeaders(
      "Access-Control-Allow-Origin" -> req.headers.get("Origin").getOrElse("*"),
      "Access-Control-Allow-Methods" -> req.headers.get("Access-Control-Request-Method").getOrElse("*"),
      "Access-Control-Allow-Headers" -> req.headers.get("Access-Control-Request-Headers").getOrElse("*"),
      "Access-Control-Allow-Credentials" -> "true"
    ))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val json = Json.toJson(JsObject(Seq(
      ("message", JsString(ex.getMessage))
    )))
    Future.successful(InternalServerError(json))
  }
}
